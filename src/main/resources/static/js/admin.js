import {apiFetch, logout, showMessage} from "/js/api.js";

let units = [];
let users = [];
let pendingAssignment = null;
document.getElementById("logout").addEventListener("click", logout);
document.getElementById("all-users").addEventListener("click", loadUsers);
document.getElementById("search-form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    await run(() => loadUsers(new FormData(form).get("q")));
});
document.getElementById("unit-form").addEventListener("submit", createUnit);
document.getElementById("edit-unit-form").addEventListener("submit", updateUnit);
document.getElementById("cancel-edit-unit").addEventListener("click", () => document.getElementById("edit-unit-dialog").close());
document.getElementById("user-form").addEventListener("submit", createUser);
document.getElementById("edit-user-form").addEventListener("submit", updateUser);
document.getElementById("cancel-edit-user").addEventListener("click", () => document.getElementById("edit-user-dialog").close());
document.getElementById("assignment-form").addEventListener("submit", saveAssignment);
document.getElementById("cancel-assignment").addEventListener("click", () => document.getElementById("assignment-dialog").close());

async function loadTree(parent = null, depth = 0) {
    const children = parent === null ? await apiFetch("/api/organization-units/roots") : await apiFetch(`/api/organization-units/${parent}/children`);
    const result = [];
    for (const unit of children) { result.push({...unit, depth}); result.push(...await loadTree(unit.id, depth + 1)); }
    return result;
}

async function loadUnits() {
    units = await loadTree();
    document.querySelectorAll(".unit-select").forEach(select => {
        const first = select.options[0]; select.replaceChildren(first);
        units.forEach(unit => select.add(new Option(`${"— ".repeat(unit.depth)}${unit.name}`, unit.id)));
    });
    renderUnitTable();
}

function renderUnitTable() {
    const body = document.getElementById("unit-table"); body.replaceChildren();
    units.forEach(unit => {
        const row = body.insertRow();
        appendCells(row, [`${"— ".repeat(unit.depth)}${unit.name}`, unit.code, unit.type, unitName(unit.parentId), managerName(unit.managerId), unit.sortOrder, unit.active ? "Так" : "Ні"]);
        const actions = row.insertCell(); actions.className = "actions";
        addButton(actions, "Редагувати", () => editUnit(unit)); addButton(actions, "Змінити батьківський", () => changeParent(unit)); addButton(actions, "Керівник", () => assignManager(unit));
        addButton(actions, unit.active ? "Деактивувати" : "Активувати", () => mutateUnit(unit.id, "active", {active: !unit.active}));
    });
}

async function loadUsers(query) {
    users = await apiFetch(query?.trim() ? `/api/users/search?q=${encodeURIComponent(query.trim())}` : "/api/users");
    populateUserSelects();
    renderUnitTable();
    const body = document.getElementById("user-table"); body.replaceChildren();
    users.forEach(user => {
        const row = body.insertRow();
        appendCells(row, [displayName(user), user.username, user.resourceNumber, user.position || "—", user.organizationUnitName || "—", user.lineManagerName || "—", user.status, user.enabled ? "Так" : "Ні", [...user.roles].join(", ")]);
        const actions = row.insertCell(); actions.className = "actions";
        addButton(actions, "Профіль", () => editProfile(user)); addButton(actions, "Підрозділ", () => assignUnit(user)); addButton(actions, "Лінійний менеджер", () => assignLineManager(user));
        addButton(actions, "Статус", () => changeStatus(user)); addButton(actions, user.enabled ? "Деактивувати" : "Активувати", () => mutateUser(user.id, "active", {active: !user.enabled}));
        addButton(actions, "+ роль", () => addRole(user)); addButton(actions, "− роль", () => removeRole(user));
    });
}

async function createUnit(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form));
    await run(async () => { await apiFetch("/api/organization-units", {method:"POST", body:JSON.stringify({...data, parentId:numberOrNull(data.parentId), sortOrder:Number(data.sortOrder)})}); form.reset(); await loadUnits(); showMessage("Організаційний підрозділ створено", "success"); });
}

async function createUser(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form));
    await run(async () => {
        await apiFetch("/api/users", {method:"POST", body:JSON.stringify({...data, organizationUnitId:numberOrNull(data.organizationUnitId), lineManagerId:numberOrNull(data.lineManagerId)})});
        form.reset();
        await loadUsers();
        showMessage("Користувача створено", "success");
    });
}

function editUnit(unit) {
    const form = document.getElementById("edit-unit-form");
    ["id", "name", "code", "type", "sortOrder"].forEach(field => { form.elements[field].value = unit[field] ?? ""; });
    document.getElementById("edit-unit-dialog").showModal();
}
async function updateUnit(event) {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(event.currentTarget));
    const id = data.id;
    delete data.id;
    data.sortOrder = Number(data.sortOrder);
    await run(async () => {
        await apiFetch(`/api/organization-units/${id}`, {method:"PUT", body:JSON.stringify(data)});
        document.getElementById("edit-unit-dialog").close();
        await loadUnits();
        showMessage("Організаційний підрозділ оновлено", "success");
    });
}
function changeParent(unit) {
    const descendantIds = descendantIdsOf(unit);
    const options = units.filter(candidate => candidate.id !== unit.id && !descendantIds.has(candidate.id)).map(candidate => ({value:candidate.id, label:`${"— ".repeat(candidate.depth)}${candidate.name}`}));
    openAssignment(`/api/organization-units/${unit.id}/parent`, "parentId", "Зміна батьківського підрозділу", "Батьківський підрозділ", "Кореневий підрозділ", unit.parentId, options, loadUnits, "Батьківський підрозділ оновлено");
}
function assignManager(unit) {
    const options = users.map(user => ({value:user.id, label:`${displayName(user)} (${user.username})`}));
    openAssignment(`/api/organization-units/${unit.id}/manager`, "managerId", "Призначення керівника", "Керівник", "Без керівника", unit.managerId, options, loadUnits, "Керівника оновлено");
}
async function mutateUnit(id, action, body) { await run(async () => { await apiFetch(`/api/organization-units/${id}/${action}`, {method:"PATCH", body:JSON.stringify(body)}); await loadUnits(); showMessage("Організаційний підрозділ оновлено", "success"); }); }

async function editProfile(user) {
    const form = document.getElementById("edit-user-form");
    const fields = ["id", "username", "resourceNumber", "grade", "firstName", "lastName", "mobileNumber", "email", "country", "city", "office", "position", "address", "authorizedPersonPhoneNumber", "timeZone"];
    fields.forEach(field => { form.elements[field].value = user[field] ?? ""; });
    document.getElementById("edit-user-dialog").showModal();
}
async function updateUser(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form));
    const id = data.id;
    delete data.id;
    await run(async () => {
        await apiFetch(`/api/users/${id}`, {method:"PUT", body:JSON.stringify(data)});
        document.getElementById("edit-user-dialog").close();
        await loadUsers();
        showMessage("Профіль користувача оновлено", "success");
    });
}
function assignUnit(user) {
    const options = units.map(unit => ({value:unit.id, label:`${"— ".repeat(unit.depth)}${unit.name}`}));
    openAssignment(`/api/users/${user.id}/organization-unit`, "organizationUnitId", "Підрозділ користувача", "Підрозділ", "Без підрозділу", user.organizationUnitId, options, loadUsers, "Користувача оновлено");
}
function assignLineManager(user) {
    const options = users.filter(manager => manager.id !== user.id).map(manager => ({value:manager.id, label:`${displayName(manager)} (${manager.username})`}));
    openAssignment(`/api/users/${user.id}/line-manager`, "lineManagerId", "Лінійний менеджер користувача", "Лінійний менеджер", "Без лінійного менеджера", user.lineManagerId, options, loadUsers, "Користувача оновлено");
}
function openAssignment(url, field, title, label, emptyLabel, currentValue, options, reload, successMessage) {
    pendingAssignment = {url, field, reload, successMessage};
    document.getElementById("assignment-title").textContent = title;
    document.getElementById("assignment-label").textContent = label;
    const select = document.getElementById("assignment-value");
    select.replaceChildren(new Option(emptyLabel, ""), ...options.map(option => new Option(option.label, option.value)));
    select.value = currentValue ?? "";
    document.getElementById("assignment-dialog").showModal();
}
async function saveAssignment(event) {
    event.preventDefault();
    if (!pendingAssignment) return;
    const value = numberOrNull(new FormData(event.currentTarget).get("value"));
    const {url, field, reload, successMessage} = pendingAssignment;
    await run(async () => {
        await apiFetch(url, {method:"PATCH", body:JSON.stringify({[field]:value})});
        document.getElementById("assignment-dialog").close();
        pendingAssignment = null;
        await reload();
        showMessage(successMessage, "success");
    });
}
async function changeStatus(user) { const value = window.prompt("Status: PENDING_EMAIL_VERIFICATION, PENDING_APPROVAL, ACTIVE, REJECTED, SUSPENDED, ARCHIVED", user.status); if (value) await mutateUser(user.id, "status", {status:value.trim().toUpperCase()}); }
async function mutateUser(id, action, body) { await run(async () => { await apiFetch(`/api/users/${id}/${action}`, {method:"PATCH", body:JSON.stringify(body)}); await loadUsers(); showMessage("Користувача оновлено", "success"); }); }
async function addRole(user) { const role = window.prompt("Роль (наприклад HEADCOUNT_MANAGER):"); if (role) await run(async () => { await apiFetch(`/api/users/${user.id}/roles`, {method:"POST", body:JSON.stringify({role:role.trim().toUpperCase()})}); await loadUsers(); }); }
async function removeRole(user) { const role = window.prompt(`Role для видалення (${[...user.roles].join(", ")}):`); if (role) await run(async () => { await apiFetch(`/api/users/${user.id}/roles/${encodeURIComponent(role.trim().toUpperCase())}`, {method:"DELETE"}); await loadUsers(); }); }

function appendCells(row, values) { values.forEach(value => { const cell = row.insertCell(); cell.textContent = value ?? ""; }); }
function addButton(container, label, action) { const button = document.createElement("button"); button.type="button"; button.textContent=label; button.addEventListener("click", action); container.append(button); }
function populateUserSelects() {
    document.querySelectorAll(".user-select").forEach(select => {
        const first = select.options[0];
        select.replaceChildren(first);
        users.forEach(user => select.add(new Option(`${displayName(user)} (${user.username})`, user.id)));
    });
}
function displayName(user) { return [user.firstName, user.lastName].filter(Boolean).join(" ") || user.username; }
function unitName(id) { return id == null ? "—" : units.find(unit => unit.id === id)?.name || "—"; }
function managerName(id) {
    if (id == null) return "—";
    const manager = users.find(user => user.id === id);
    return manager ? `${displayName(manager)} (${manager.username})` : "—";
}
function descendantIdsOf(unit) {
    const descendants = new Set();
    const start = units.findIndex(candidate => candidate.id === unit.id);
    for (let index = start + 1; index < units.length && units[index].depth > unit.depth; index++) descendants.add(units[index].id);
    return descendants;
}
function numberOrNull(value) { return value === "" || value == null ? null : Number(value); }
async function run(action) { try { await action(); } catch (error) { showMessage(error.message, "error"); } }

run(async () => { await loadUnits(); await loadUsers(); });
