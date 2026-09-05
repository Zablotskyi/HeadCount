import {apiFetch, logout, showMessage} from "/js/api.js";

const headcountLifecycleRoles = new Set(["ADMIN", "HEADCOUNT_MANAGER"]);
const applicationTimeZone = "Europe/Kyiv";
let currentUser;
let units = [];
let activeEvent;
let activeHeadcountUnitIds = new Set();
let activeHeadcountAncestorIds = new Set();
let activeHeadcountByUnitId = new Map();
const expandedUnitIds = new Set();
const participantCache = new Map();
const participantLoadPromises = new Map();
let pendingEventAction = null;

document.getElementById("logout").addEventListener("click", logout);
document.getElementById("refresh").addEventListener("click", refreshIndex);
document.getElementById("start-headcount").addEventListener("click", startHeadcount);
document.getElementById("scope-select").addEventListener("change", refreshEvent);
document.getElementById("event-action-back").addEventListener("click", closeEventActionDialog);
document.getElementById("event-action-confirm").addEventListener("click", executeEventAction);
document.getElementById("close-participant-details").addEventListener("click", () => document.getElementById("participant-details-dialog").close());

async function loadTree(parent = null, depth = 0) {
    const children = parent === null
        ? await apiFetch("/api/organization-units/roots")
        : await apiFetch(`/api/organization-units/${parent}/children`);
    const result = [];
    for (const unit of children) {
        result.push({...unit, depth});
        result.push(...await loadTree(unit.id, depth + 1));
    }
    return result;
}

function renderScopeSelect() {
    const select = document.getElementById("scope-select");
    select.replaceChildren();
    for (const unit of units) {
        select.add(new Option(`${"— ".repeat(unit.depth)}${unit.name}`, unit.id));
    }
    if (currentUser.organizationUnitId && units.some(unit => unit.id === currentUser.organizationUnitId)) {
        select.value = String(currentUser.organizationUnitId);
    }
}

function renderOrganizationTree() {
    const tree = document.getElementById("organization-tree");
    tree.replaceChildren();
    const byParentId = new Map();
    const byId = new Map(units.map(unit => [unit.id, unit]));
    for (const unit of units) {
        const parentId = byId.has(unit.parentId) ? unit.parentId : null;
        const children = byParentId.get(parentId) || [];
        children.push(unit);
        byParentId.set(parentId, children);
    }
    const rendered = new Set();
    for (const root of byParentId.get(null) || []) {
        tree.append(renderOrganizationUnit(root, byParentId, rendered));
    }
}

function renderOrganizationUnit(unit, byParentId, rendered) {
    rendered.add(unit.id);
    const wrapper = document.createElement("section");
    wrapper.className = "organization-unit";
    const expanded = expandedUnitIds.has(unit.id);
    const button = document.createElement("button");
    button.type = "button";
    button.className = "unit";
    button.setAttribute("aria-expanded", String(expanded));
    button.addEventListener("click", () => toggleOrganizationUnit(unit));

    const marker = document.createElement("span");
    marker.className = "unit-marker";
    marker.textContent = expanded ? "▼" : "▶";
    const label = document.createElement("span");
    label.textContent = `${unit.name} (${unit.type})${unit.active ? "" : " — неактивна"}`;
    button.append(marker, label);
    if (activeHeadcountUnitIds.has(unit.id)) {
        button.classList.add("headcount-active");
        const badge = document.createElement("span");
        badge.className = "headcount-active-badge";
        badge.textContent = "HeadCount активний";
        button.append(badge);
    } else if (activeHeadcountAncestorIds.has(unit.id)) {
        button.classList.add("headcount-descendant-active");
        button.title = "Активний HeadCount є в дочірньому підрозділі";
        button.setAttribute("aria-label", `${unit.name}: активний HeadCount є в дочірньому підрозділі`);
    }
    wrapper.append(button);

    if (expanded) {
        const content = document.createElement("div");
        content.className = "unit-content";
        content.append(renderUnitParticipants(unit));
        const children = document.createElement("div");
        children.className = "unit-children";
        for (const child of byParentId.get(unit.id) || []) {
            if (!rendered.has(child.id)) children.append(renderOrganizationUnit(child, byParentId, rendered));
        }
        content.append(children);
        wrapper.append(content);
    }
    return wrapper;
}

async function toggleOrganizationUnit(unit) {
    if (expandedUnitIds.delete(unit.id)) {
        renderOrganizationTree();
        return;
    }
    expandedUnitIds.add(unit.id);
    renderOrganizationTree();
    const eventId = findActiveEventIdForUnit(unit.id);
    if (eventId != null) {
        try {
            await loadParticipantsForEvent(eventId);
        } catch (error) {
            showMessage(error.message, "error");
        }
        renderOrganizationTree();
    }
}

function findActiveEventIdForUnit(unitId) {
    const byId = new Map(units.map(unit => [unit.id, unit]));
    let currentId = unitId;
    const visited = new Set();
    while (currentId != null && !visited.has(currentId)) {
        visited.add(currentId);
        if (activeHeadcountByUnitId.has(currentId)) return activeHeadcountByUnitId.get(currentId);
        currentId = byId.get(currentId)?.parentId;
    }
    return null;
}

async function loadParticipantsForEvent(eventId) {
    if (participantCache.has(eventId)) return participantCache.get(eventId);
    if (!participantLoadPromises.has(eventId)) {
        const request = apiFetch(`/api/headcount/events/${eventId}/participants`)
            .then(participants => {
                participantCache.set(eventId, participants);
                return participants;
            })
            .finally(() => participantLoadPromises.delete(eventId));
        participantLoadPromises.set(eventId, request);
    }
    return participantLoadPromises.get(eventId);
}

async function refreshActiveSummary() {
    const summary = await apiFetch("/api/headcount/events/active-summary");
    activeHeadcountByUnitId = new Map(summary.map(item => [item.organizationUnitId, item.eventId]));
    activeHeadcountUnitIds = new Set(summary.map(item => item.organizationUnitId));
    activeHeadcountAncestorIds = findActiveAncestorIds(units, activeHeadcountUnitIds);
    participantCache.clear();
    participantLoadPromises.clear();
    renderOrganizationTree();

    const expandedEventIds = new Set([...expandedUnitIds]
        .map(findActiveEventIdForUnit)
        .filter(eventId => eventId != null));
    if (expandedEventIds.size > 0) {
        await Promise.all([...expandedEventIds].map(loadParticipantsForEvent));
        renderOrganizationTree();
    }
}

function findActiveAncestorIds(allUnits, activeUnitIds) {
    const byId = new Map(allUnits.map(unit => [unit.id, unit]));
    const ancestorIds = new Set();
    for (const activeUnitId of activeUnitIds) {
        let parentId = byId.get(activeUnitId)?.parentId;
        const visited = new Set();
        while (parentId != null && !visited.has(parentId)) {
            visited.add(parentId);
            ancestorIds.add(parentId);
            parentId = byId.get(parentId)?.parentId;
        }
    }
    return ancestorIds;
}

async function refreshIndex() {
    await refreshActiveSummary();
    await refreshEvent();
}

async function refreshEvent() {
    const scopeId = document.getElementById("scope-select").value;
    activeEvent = await apiFetch(`/api/headcount/events/active${scopeId ? `?scopeOrganizationUnitId=${encodeURIComponent(scopeId)}` : ""}`);
    const summary = document.getElementById("active-event");
    if (!activeEvent) {
        summary.replaceChildren();
        const empty = document.createElement("strong");
        empty.textContent = "Активної події для вибраної області немає";
        summary.append(empty);
        document.getElementById("start-headcount").hidden = !canManageHeadcount();
        return;
    }
    renderActiveEvent();
    document.getElementById("start-headcount").hidden = true;
}

function renderActiveEvent() {
    const summary = document.getElementById("active-event");
    summary.replaceChildren();
    const details = document.createElement("div");
    details.className = "event-details";
    const title = document.createElement("strong");
    title.textContent = activeEvent.title;
    const scope = document.createElement("span");
    scope.textContent = activeEvent.scopeOrganizationUnitName;
    const status = document.createElement("span");
    status.textContent = `Статус: ${activeEvent.status}`;
    const created = document.createElement("span");
    created.textContent = `Створено: ${formatTime(activeEvent.createdAt)}`;
    details.append(title, scope, status, created);
    summary.append(details);
    if (activeEvent.status === "ACTIVE" && canManageHeadcount()) {
        const actions = document.createElement("div");
        actions.className = "event-actions";
        const close = document.createElement("button");
        close.type = "button";
        close.textContent = "Закрити HeadCount";
        close.addEventListener("click", () => openEventActionDialog("close"));
        const cancel = document.createElement("button");
        cancel.type = "button";
        cancel.className = "danger";
        cancel.textContent = "Скасувати HeadCount";
        cancel.addEventListener("click", () => openEventActionDialog("cancel"));
        actions.append(close, cancel);
        summary.append(actions);
    }
}

function openEventActionDialog(action) {
    if (!activeEvent || activeEvent.status !== "ACTIVE" || !canManageHeadcount()) return;
    pendingEventAction = {action, eventId:activeEvent.id};
    const isClose = action === "close";
    document.getElementById("event-action-title").textContent = isClose ? "Закрити HeadCount" : "Скасувати HeadCount";
    document.getElementById("event-action-message").textContent = isClose
        ? "Закрити цей HeadCount? Після закриття підтвердження більше не прийматимуться."
        : "Скасувати цей HeadCount? Використовуйте це, якщо подію було оголошено помилково.";
    document.getElementById("event-action-back").textContent = isClose ? "Скасувати" : "Назад";
    const confirmButton = document.getElementById("event-action-confirm");
    confirmButton.textContent = isClose ? "Закрити" : "Скасувати HeadCount";
    confirmButton.className = isClose ? "" : "danger";
    document.getElementById("event-action-dialog").showModal();
}

function closeEventActionDialog() {
    pendingEventAction = null;
    document.getElementById("event-action-dialog").close();
}

async function executeEventAction() {
    if (!pendingEventAction) return;
    const {action, eventId} = pendingEventAction;
    try {
        await apiFetch(`/api/headcount/events/${eventId}/${action}`, {method:"POST"});
        closeEventActionDialog();
        showMessage(action === "close" ? "HeadCount закрито" : "HeadCount скасовано", "success");
        await refreshIndex();
    } catch (error) {
        closeEventActionDialog();
        await refreshIndex().catch(() => {});
        showMessage(error.message, "error");
    }
}

function renderUnitParticipants(unit) {
    const container = document.createElement("div");
    container.className = "unit-participants";
    const eventId = findActiveEventIdForUnit(unit.id);
    if (eventId == null) {
        container.append(emptyParticipantMessage());
        return container;
    }
    if (!participantCache.has(eventId)) {
        const loading = document.createElement("span");
        loading.className = "muted";
        loading.textContent = "Завантаження учасників...";
        container.append(loading);
        return container;
    }
    const participants = participantCache.get(eventId)
        .filter(participant => participant.organizationUnitId === unit.id);
    if (participants.length === 0) {
        container.append(emptyParticipantMessage());
        return container;
    }
    const hierarchy = buildParticipantHierarchy(participants, unit.managerId);
    renderParticipantTree(container, hierarchy, eventId);
    return container;
}

function emptyParticipantMessage() {
    const empty = document.createElement("span");
    empty.className = "muted";
    empty.textContent = "Немає учасників активного HeadCount";
    return empty;
}

function buildParticipantHierarchy(participants, unitManagerId) {
    const byEmployeeId = new Map(participants.map(participant => [participant.employeeId, participant]));
    const childrenByManagerId = new Map();
    const roots = [];
    for (const participant of participants) {
        const managerIsVisible = participant.lineManagerId != null
            && byEmployeeId.has(participant.lineManagerId);
        const isUnitManager = participant.employeeId === unitManagerId;
        if (!managerIsVisible || isUnitManager) {
            roots.push(participant);
        } else {
            const children = childrenByManagerId.get(participant.lineManagerId) || [];
            children.push(participant);
            childrenByManagerId.set(participant.lineManagerId, children);
        }
    }
    roots.sort(participantComparator);
    for (const children of childrenByManagerId.values()) children.sort(participantComparator);
    if (unitManagerId != null) {
        roots.sort((left, right) => Number(right.employeeId === unitManagerId)
            - Number(left.employeeId === unitManagerId) || participantComparator(left, right));
    }
    return {participants: [...participants].sort(participantComparator), roots, childrenByManagerId};
}

function renderParticipantTree(container, hierarchy, eventId) {
    const visited = new Set();
    for (const root of hierarchy.roots) {
        const node = renderParticipantNode(root, hierarchy.childrenByManagerId, visited, eventId, 0);
        if (node) container.append(node);
    }
    for (const participant of hierarchy.participants) {
        if (visited.has(participant.employeeId)) continue;
        const node = renderParticipantNode(participant, hierarchy.childrenByManagerId, visited, eventId, 0);
        if (node) container.append(node);
    }
}

function renderParticipantNode(participant, childrenByManagerId, visited, eventId, depth) {
    if (visited.has(participant.employeeId)) return null;
    visited.add(participant.employeeId);
    const node = document.createElement("div");
    node.className = "participant-node";
    node.style.setProperty("--employee-depth", depth);
    node.append(renderParticipantCard(participant, eventId));
    for (const child of childrenByManagerId.get(participant.employeeId) || []) {
        const childNode = renderParticipantNode(child, childrenByManagerId, visited, eventId, depth + 1);
        if (childNode) node.append(childNode);
    }
    return node;
}

function renderParticipantCard(participant, eventId) {
        const card = document.createElement("article"); card.className = `participant ${participant.status}`;
        card.tabIndex = 0;
        card.setAttribute("role", "button");
        card.setAttribute("aria-label", `Переглянути дані учасника ${participant.employeeNameSnapshot}`);
        card.addEventListener("click", event => {
            event.stopPropagation();
            openParticipantDetails(participant, eventId);
        });
        card.addEventListener("keydown", event => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                event.stopPropagation();
                openParticipantDetails(participant, eventId);
            }
        });
        card.innerHTML = `<strong></strong><div class="muted"></div><div class="status"></div><div class="confirmer muted"></div><div class="help"></div>`;
        card.querySelector("strong").textContent = participantName(participant);
        card.querySelector(".muted").textContent = [participant.position, participant.resourceNumberSnapshot]
            .filter(Boolean).join(" · ");
        card.querySelector(".status").textContent = `${participant.status}${participant.confirmedAt ? ` · ${formatTime(participant.confirmedAt)}` : ""}`;
        if (participant.status !== "PENDING" && participant.confirmedById != null
                && participant.confirmedById !== participant.employeeId) {
            const confirmerName = [participant.confirmedByFirstName, participant.confirmedByLastName]
                .map(value => value?.trim()).filter(Boolean).join(" ") || "—";
            card.querySelector(".confirmer").textContent = `Підтвердив: ${confirmerName}`;
        }
        if (participant.status === "NEED_HELP") card.querySelector(".help").textContent = participant.helpMessage || "Потрібна допомога";
        if (canConfirmParticipant(participant)) {
            const actions = document.createElement("div"); actions.className = "participant-actions";
            const safe = document.createElement("button"); safe.textContent = "Я в безпеці"; safe.onclick = event => { event.stopPropagation(); confirm(participant, eventId, "safe"); };
            const help = document.createElement("button"); help.textContent = "Потрібна допомога"; help.onclick = event => { event.stopPropagation(); confirm(participant, eventId, "need-help"); };
            safe.addEventListener("keydown", event => event.stopPropagation());
            help.addEventListener("keydown", event => event.stopPropagation());
            actions.append(safe, help); card.append(actions);
        }
        return card;
}

function participantName(participant) {
    return [participant.employeeFirstName, participant.employeeLastName]
        .map(value => value?.trim()).filter(Boolean).join(" ") || participant.employeeNameSnapshot;
}

function participantComparator(left, right) {
    return participantName(left).localeCompare(participantName(right), "uk", {sensitivity: "base"});
}

async function openParticipantDetails(participant, eventId) {
    const dialog = document.getElementById("participant-details-dialog");
    const content = document.getElementById("participant-details-content");
    content.className = "muted";
    content.textContent = "Завантаження...";
    dialog.showModal();
    try {
        const details = await apiFetch(`/api/headcount/events/${eventId}/participants/${participant.id}`);
        renderParticipantDetails(details);
    } catch (error) {
        dialog.close();
        showMessage(error.message, "error");
    }
}

function renderParticipantDetails(details) {
    const fields = [
        ["Ім’я", "firstName"], ["Прізвище", "lastName"], ["Посада", "position"],
        ["Email", "email"], ["Мобільний телефон", "mobileNumber"], ["Країна", "country"],
        ["Місто", "city"], ["Офіс", "office"], ["Адреса", "address"]
    ];
    const list = document.createElement("dl");
    list.className = "participant-details";
    fields.forEach(([label, field]) => {
        const item = document.createElement("div");
        item.className = "participant-detail";
        const term = document.createElement("dt");
        term.textContent = label;
        const value = document.createElement("dd");
        value.textContent = details[field]?.toString().trim() || "—";
        item.append(term, value);
        list.append(item);
    });
    const content = document.getElementById("participant-details-content");
    content.className = "";
    content.replaceChildren(list);
}

async function confirm(participant, eventId, action) {
    try {
        const body = {confirmationSource: "WEB"};
        if (action === "need-help") {
            const message = window.prompt("Опишіть, яка допомога потрібна:");
            if (!message?.trim()) return;
            body.helpMessage = message.trim();
        }
        await apiFetch(`/api/headcount/events/${eventId}/participants/${participant.employeeId}/${action}`, {method: "POST", body: JSON.stringify(body)});
        participantCache.delete(eventId);
        showMessage("Статус оновлено", "success");
        await loadParticipantsForEvent(eventId);
        renderOrganizationTree();
    } catch (error) { showMessage(error.message, "error"); }
}

async function startHeadcount() {
    try {
        const scopeOrganizationUnitId = Number(document.getElementById("scope-select").value);
        const title = document.getElementById("event-title").value.trim();
        activeEvent = await apiFetch("/api/headcount/events", {method: "POST", body: JSON.stringify({title, description: "Оголошено через web interface", scopeOrganizationUnitId})});
        showMessage("HeadCount оголошено", "success"); await refreshIndex();
    } catch (error) { showMessage(error.message, "error"); }
}

function canManageHeadcount() { return currentUser?.roles.some(role => headcountLifecycleRoles.has(role)) ?? false; }
function canConfirmParticipant(participant) {
    return participant.employeeId === currentUser?.id
        || canManageHeadcount()
        || isInCurrentUsersManagedBranch(participant.organizationUnitId);
}
function isInCurrentUsersManagedBranch(organizationUnitId) {
    if (organizationUnitId == null || currentUser?.id == null) return false;
    const byId = new Map(units.map(unit => [unit.id, unit]));
    let currentId = organizationUnitId;
    const visited = new Set();
    while (currentId != null && !visited.has(currentId)) {
        visited.add(currentId);
        const unit = byId.get(currentId);
        if (!unit) return false;
        if (unit.managerId === currentUser.id) return true;
        currentId = unit.parentId;
    }
    return false;
}
function formatTime(value) {
    if (!value) return "";
    const instant = parseBackendTimestamp(value);
    if (!instant || Number.isNaN(instant.getTime())) return "";
    const options = {day:"2-digit", month:"2-digit", year:"numeric", hour:"2-digit", minute:"2-digit"};
    try {
        return new Intl.DateTimeFormat("uk-UA", {...options, timeZone: currentUser?.timeZone || applicationTimeZone}).format(instant);
    } catch (error) {
        if (!(error instanceof RangeError)) throw error;
        return new Intl.DateTimeFormat("uk-UA", {...options, timeZone: applicationTimeZone}).format(instant);
    }
}

function parseBackendTimestamp(value) {
    return new Date(value);
}

async function init() {
    try {
        currentUser = await apiFetch("/api/users/me");
        document.getElementById("current-user").textContent = `${currentUser.firstName} ${currentUser.lastName}`.trim() || currentUser.username;
        document.getElementById("admin-link").hidden = !currentUser.roles.includes("ADMIN");
        document.getElementById("start-headcount").hidden = !canManageHeadcount();
        units = await loadTree(); renderScopeSelect(); await refreshIndex();
    } catch (error) { showMessage(error.message, "error"); }
}

init();
