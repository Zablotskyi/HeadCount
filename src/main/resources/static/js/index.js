import {apiFetch, logout, showMessage} from "/js/api.js";

const headcountLifecycleRoles = new Set(["ADMIN", "HEADCOUNT_MANAGER"]);
let currentUser;
let units = [];
let activeEvent;
let pendingEventAction = null;

document.getElementById("logout").addEventListener("click", logout);
document.getElementById("refresh").addEventListener("click", refreshEvent);
document.getElementById("start-headcount").addEventListener("click", startHeadcount);
document.getElementById("scope-select").addEventListener("change", refreshEvent);
document.getElementById("event-action-back").addEventListener("click", closeEventActionDialog);
document.getElementById("event-action-confirm").addEventListener("click", executeEventAction);

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

function renderUnits() {
    const tree = document.getElementById("organization-tree");
    const select = document.getElementById("scope-select");
    tree.replaceChildren(); select.replaceChildren();
    for (const unit of units) {
        const node = document.createElement("div");
        node.className = "unit"; node.style.setProperty("--depth", unit.depth);
        node.textContent = `${unit.name} (${unit.type})${unit.active ? "" : " — неактивна"}`;
        tree.append(node);
        const option = new Option(`${"— ".repeat(unit.depth)}${unit.name}`, unit.id);
        select.add(option);
    }
    if (currentUser.organizationUnitId && units.some(unit => unit.id === currentUser.organizationUnitId)) {
        select.value = String(currentUser.organizationUnitId);
    }
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
        document.getElementById("participants").replaceChildren();
        document.getElementById("start-headcount").hidden = !canManageHeadcount();
        return;
    }
    renderActiveEvent();
    document.getElementById("start-headcount").hidden = true;
    renderParticipants(await apiFetch(`/api/headcount/events/${activeEvent.id}/participants`));
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
        await refreshEvent();
    } catch (error) {
        closeEventActionDialog();
        await refreshEvent().catch(() => {});
        showMessage(error.message, "error");
    }
}

function renderParticipants(participants) {
    const container = document.getElementById("participants"); container.replaceChildren();
    for (const participant of participants) {
        const card = document.createElement("article"); card.className = `participant ${participant.status}`;
        card.innerHTML = `<strong></strong><div class="muted"></div><div class="status"></div><div class="help"></div>`;
        card.querySelector("strong").textContent = participant.employeeNameSnapshot;
        card.querySelector(".muted").textContent = `${participant.resourceNumberSnapshot} · ${participant.organizationPathSnapshot}`;
        card.querySelector(".status").textContent = `${participant.status}${participant.confirmedAt ? ` · ${formatTime(participant.confirmedAt)}` : ""}`;
        if (participant.status === "NEED_HELP") card.querySelector(".help").textContent = participant.helpMessage || "Потрібна допомога";
        if (activeEvent?.status === "ACTIVE") {
            const actions = document.createElement("div"); actions.className = "participant-actions";
            const safe = document.createElement("button"); safe.textContent = "Я в безпеці"; safe.onclick = () => confirm(participant, "safe");
            const help = document.createElement("button"); help.textContent = "Потрібна допомога"; help.onclick = () => confirm(participant, "need-help");
            actions.append(safe, help); card.append(actions);
        }
        container.append(card);
    }
}

async function confirm(participant, action) {
    try {
        const body = {confirmationSource: "WEB"};
        if (action === "need-help") {
            const message = window.prompt("Опишіть, яка допомога потрібна:");
            if (!message?.trim()) return;
            body.helpMessage = message.trim();
        }
        await apiFetch(`/api/headcount/events/${activeEvent.id}/participants/${participant.employeeId}/${action}`, {method: "POST", body: JSON.stringify(body)});
        showMessage("Статус оновлено", "success"); await refreshEvent();
    } catch (error) { showMessage(error.message, "error"); }
}

async function startHeadcount() {
    try {
        const scopeOrganizationUnitId = Number(document.getElementById("scope-select").value);
        const title = document.getElementById("event-title").value.trim();
        activeEvent = await apiFetch("/api/headcount/events", {method: "POST", body: JSON.stringify({title, description: "Оголошено через web interface", scopeOrganizationUnitId})});
        showMessage("HeadCount оголошено", "success"); await refreshEvent();
    } catch (error) { showMessage(error.message, "error"); }
}

function canManageHeadcount() { return currentUser?.roles.some(role => headcountLifecycleRoles.has(role)) ?? false; }
function formatTime(value) { return value ? new Intl.DateTimeFormat("uk-UA", {day:"2-digit", month:"2-digit", year:"numeric", hour:"2-digit", minute:"2-digit"}).format(new Date(`${value}Z`)) : ""; }

async function init() {
    try {
        currentUser = await apiFetch("/api/users/me");
        document.getElementById("current-user").textContent = `${currentUser.firstName} ${currentUser.lastName}`.trim() || currentUser.username;
        document.getElementById("admin-link").hidden = !currentUser.roles.includes("ADMIN");
        document.getElementById("start-headcount").hidden = !canManageHeadcount();
        units = await loadTree(); renderUnits(); await refreshEvent();
    } catch (error) { showMessage(error.message, "error"); }
}

init();
