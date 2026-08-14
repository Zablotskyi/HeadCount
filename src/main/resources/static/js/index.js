import {apiFetch, logout, showMessage} from "/js/api.js";

const managementRoles = new Set(["COUNTRY_MANAGER", "REGIONAL_MANAGER", "SUPPORT_MANAGER", "PROGRAM_MANAGER", "DEPARTMENT_MANAGER", "UNIT_MANAGER", "SECURITY_OFFICER", "SECURITY_MANAGER", "ADMIN"]);
let currentUser;
let units = [];
let activeEvent;

document.getElementById("logout").addEventListener("click", logout);
document.getElementById("refresh").addEventListener("click", refreshEvent);
document.getElementById("start-headcount").addEventListener("click", startHeadcount);
document.getElementById("scope-select").addEventListener("change", refreshEvent);

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
        summary.innerHTML = "<strong>Активної події для вибраної області немає</strong>";
        document.getElementById("participants").replaceChildren();
        return;
    }
    summary.textContent = `${activeEvent.title} · ${activeEvent.scopeOrganizationUnitName} · ${formatTime(activeEvent.startedAt)}`;
    renderParticipants(await apiFetch(`/api/headcount/events/${activeEvent.id}/participants`));
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

function formatTime(value) { return value ? new Intl.DateTimeFormat("uk-UA", {dateStyle:"short", timeStyle:"medium"}).format(new Date(`${value}Z`)) : ""; }

async function init() {
    try {
        currentUser = await apiFetch("/api/users/me");
        document.getElementById("current-user").textContent = `${currentUser.firstName} ${currentUser.lastName}`.trim() || currentUser.username;
        document.getElementById("admin-link").hidden = !currentUser.roles.includes("ADMIN");
        document.getElementById("start-headcount").hidden = !currentUser.roles.some(role => managementRoles.has(role));
        units = await loadTree(); renderUnits(); await refreshEvent();
    } catch (error) { showMessage(error.message, "error"); }
}

init();
