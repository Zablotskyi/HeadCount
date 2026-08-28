import {apiFetch, logout, showMessage} from "/js/api.js";

let units = [];
let users = [];
let availableRoles = [];
let currentUser = null;
let pendingAssignment = null;
let pendingRoleAction = null;
let pendingStatusChange = null;
const userStatuses = [
    {value:"PENDING_EMAIL_VERIFICATION", label:"Очікує підтвердження email"},
    {value:"PENDING_APPROVAL", label:"Очікує схвалення"},
    {value:"ACTIVE", label:"Активний", description:"Користувач може працювати в системі за умови enabled=true."},
    {value:"REJECTED", label:"Відхилений"},
    {value:"SUSPENDED", label:"Призупинений", description:"Обліковий запис тимчасово призупинено."},
    {value:"ARCHIVED", label:"Архівний", description:"Користувач більше не використовується активно."}
];
document.getElementById("logout").addEventListener("click", logout);
document.getElementById("search-form").addEventListener("submit", event => event.preventDefault());
document.getElementById("user-search").addEventListener("input", applyUserFilter);
document.getElementById("unit-search").addEventListener("input", applyUnitFilter);
document.getElementById("unit-form").addEventListener("submit", createUnit);
document.getElementById("edit-unit-form").addEventListener("submit", updateUnit);
document.getElementById("cancel-edit-unit").addEventListener("click", () => document.getElementById("edit-unit-dialog").close());
document.getElementById("user-form").addEventListener("submit", createUser);
document.getElementById("edit-user-form").addEventListener("submit", updateUser);
document.getElementById("cancel-edit-user").addEventListener("click", () => document.getElementById("edit-user-dialog").close());
document.getElementById("assignment-form").addEventListener("submit", saveAssignment);
document.getElementById("cancel-assignment").addEventListener("click", () => document.getElementById("assignment-dialog").close());
document.getElementById("assignment-search").addEventListener("input", renderAssignmentOptions);
document.getElementById("assignment-clear").addEventListener("change", selectAssignmentValue);
document.getElementById("role-form").addEventListener("submit", saveRole);
document.getElementById("role-search").addEventListener("input", renderRoleOptions);
document.getElementById("cancel-role").addEventListener("click", closeRoleDialog);
document.getElementById("status-form").addEventListener("submit", saveStatus);
document.getElementById("cancel-status").addEventListener("click", closeStatusDialog);

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
    applyUnitFilter();
}

function applyUnitFilter() {
    const tokens = document.getElementById("unit-search").value.trim().toLocaleLowerCase().split(/\s+/).filter(Boolean);
    const filteredUnits = tokens.length ? units.filter(unit => {
        const searchableText = [
            unit.name, unit.code, unit.type, unitParentPath(unit), managerName(unit.managerId), unit.sortOrder
        ].filter(value => value != null).join(" ").toLocaleLowerCase();
        return tokens.every(token => searchableText.includes(token));
    }) : units;
    renderUnitTable(filteredUnits);
}

function renderUnitTable(list = units) {
    const body = document.getElementById("unit-table"); body.replaceChildren();
    if (!list.length) {
        const cell = body.insertRow().insertCell();
        cell.colSpan = 8;
        cell.textContent = "Підрозділів не знайдено";
        cell.className = "muted";
        return;
    }
    list.forEach(unit => {
        const row = body.insertRow();
        appendCells(row, [`${"— ".repeat(unit.depth)}${unit.name}`, unit.code, unit.type, unitName(unit.parentId), managerName(unit.managerId), unit.sortOrder, unit.active ? "Так" : "Ні"]);
        const actions = row.insertCell(); actions.className = "actions";
        addButton(actions, "Редагувати", () => editUnit(unit)); addButton(actions, "Змінити батьківський", () => changeParent(unit)); addButton(actions, "Керівник", () => assignManager(unit));
        addButton(actions, unit.active ? "Деактивувати" : "Активувати", () => mutateUnit(unit.id, "active", {active: !unit.active}));
    });
}

async function loadUsers() {
    users = await apiFetch("/api/users");
    populateUserSelects();
    applyUnitFilter();
    applyUserFilter();
}

function applyUserFilter() {
    const tokens = document.getElementById("user-search").value.trim().toLocaleLowerCase().split(/\s+/).filter(Boolean);
    const filteredUsers = tokens.length ? users.filter(user => {
        const searchableText = [
            user.firstName, user.lastName, user.username, user.email, user.resourceNumber,
            user.position, user.organizationUnitName, user.lineManagerName, user.status,
            ...(user.roles || [])
        ].filter(value => value != null).join(" ").toLocaleLowerCase();
        return tokens.every(token => searchableText.includes(token));
    }) : users;
    renderUsersTable(filteredUsers);
}

function renderUsersTable(list) {
    const body = document.getElementById("user-table"); body.replaceChildren();
    if (!list.length) {
        const cell = body.insertRow().insertCell();
        cell.colSpan = 10;
        cell.textContent = "Користувачів не знайдено";
        cell.className = "muted";
        return;
    }
    list.forEach(user => {
        const row = body.insertRow();
        appendCells(row, [displayName(user), user.username, user.resourceNumber, user.position || "—", user.organizationUnitName || "—", user.lineManagerName || "—", user.status, user.enabled ? "Так" : "Ні", [...user.roles].join(", ")]);
        const actions = row.insertCell(); actions.className = "actions";
        addButton(actions, "Профіль", () => editProfile(user)); addButton(actions, "Підрозділ", () => assignUnit(user)); addButton(actions, "Лінійний менеджер", () => assignLineManager(user));
        addButton(actions, "Статус", () => changeStatus(user)); addButton(actions, user.enabled ? "Деактивувати" : "Активувати", () => mutateUser(user.id, "active", {active: !user.enabled}));
        addButton(actions, "+ роль", () => addRole(user)); addButton(actions, "− роль", () => removeRole(user));
    });
}

async function loadRoles() { availableRoles = await apiFetch("/api/roles"); }
async function loadCurrentUser() { currentUser = await apiFetch("/api/users/me"); }

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
    const options = units.filter(candidate => candidate.id !== unit.id && !descendantIds.has(candidate.id)).map(unitOption);
    openAssignment({url:`/api/organization-units/${unit.id}/parent`, field:"parentId", title:"Зміна батьківського підрозділу", subject:unit.name, placeholder:"Пошук підрозділу...", clearLabel:"Кореневий підрозділ", emptyMessage:"Підрозділів не знайдено", currentValue:unit.parentId, options, reload:loadUnits, successMessage:"Батьківський підрозділ оновлено"});
}
function assignManager(unit) {
    const options = users.map(userOption);
    openAssignment({url:`/api/organization-units/${unit.id}/manager`, field:"managerId", title:"Керівник підрозділу", subject:unit.name, placeholder:"Пошук співробітника...", clearLabel:"Без керівника", emptyMessage:"Співробітників не знайдено", currentValue:unit.managerId, options, reload:loadUnits, successMessage:"Керівника оновлено"});
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
    openAssignment({url:`/api/users/${user.id}/organization-unit`, field:"organizationUnitId", title:"Підрозділ користувача", subject:`${displayName(user)} (${user.username})`, placeholder:"Пошук підрозділу...", clearLabel:"Без підрозділу", emptyMessage:"Підрозділів не знайдено", currentValue:user.organizationUnitId, options:units.map(unitOption), reload:loadUsers, successMessage:"Користувача оновлено"});
}
function assignLineManager(user) {
    const options = users.filter(manager => manager.id !== user.id).map(userOption);
    openAssignment({url:`/api/users/${user.id}/line-manager`, field:"lineManagerId", title:"Лінійний менеджер користувача", subject:`${displayName(user)} (${user.username})`, placeholder:"Пошук співробітника...", clearLabel:"Без лінійного менеджера", emptyMessage:"Співробітників не знайдено", currentValue:user.lineManagerId, options, reload:loadUsers, successMessage:"Користувача оновлено"});
}
function openAssignment({url, field, title, subject, placeholder, clearLabel, emptyMessage, currentValue, options, reload, successMessage}) {
    pendingAssignment = {url, field, reload, successMessage, options, emptyMessage, currentValue:String(currentValue ?? ""), selectedValue:String(currentValue ?? "")};
    document.getElementById("assignment-title").textContent = title;
    const subjectElement = document.getElementById("assignment-subject");
    subjectElement.textContent = subject || "";
    subjectElement.hidden = !subject;
    const search = document.getElementById("assignment-search");
    search.value = "";
    search.placeholder = placeholder;
    document.getElementById("assignment-clear-label").textContent = clearLabel;
    document.querySelector("#assignment-clear input").checked = pendingAssignment.currentValue === "";
    document.getElementById("submit-assignment").disabled = true;
    renderAssignmentOptions();
    document.getElementById("assignment-dialog").showModal();
    search.focus();
}
function renderAssignmentOptions() {
    if (!pendingAssignment) return;
    const query = document.getElementById("assignment-search").value.trim().toLocaleLowerCase();
    const filtered = pendingAssignment.options.filter(option => option.searchText.toLocaleLowerCase().includes(query));
    const list = document.getElementById("assignment-list");
    list.replaceChildren();
    if (!filtered.length) {
        const empty = document.createElement("div");
        empty.className = "role-empty";
        empty.textContent = pendingAssignment.emptyMessage;
        list.append(empty);
        return;
    }
    filtered.forEach(option => {
        const label = document.createElement("label");
        label.className = "assignment-option";
        const radio = document.createElement("input");
        radio.type = "radio";
        radio.name = "value";
        radio.value = option.value;
        radio.checked = pendingAssignment.selectedValue === String(option.value);
        radio.addEventListener("change", selectAssignmentValue);
        const text = document.createElement("span");
        text.className = "assignment-option-text";
        text.append(document.createTextNode(option.label));
        if (option.detail) {
            const detail = document.createElement("span");
            detail.className = "assignment-detail";
            detail.textContent = option.detail;
            text.append(detail);
        }
        label.append(radio, text);
        list.append(label);
    });
}
function selectAssignmentValue(event) {
    if (!pendingAssignment || !event.target.checked) return;
    pendingAssignment.selectedValue = event.target.value;
    document.getElementById("submit-assignment").disabled = pendingAssignment.selectedValue === pendingAssignment.currentValue;
}
async function saveAssignment(event) {
    event.preventDefault();
    if (!pendingAssignment) return;
    const value = numberOrNull(pendingAssignment.selectedValue);
    const {url, field, reload, successMessage} = pendingAssignment;
    await run(async () => {
        await apiFetch(url, {method:"PATCH", body:JSON.stringify({[field]:value})});
        document.getElementById("assignment-dialog").close();
        pendingAssignment = null;
        await reload();
        showMessage(successMessage, "success");
    });
}
function changeStatus(user) {
    pendingStatusChange = {user, selectedStatus:user.status};
    document.getElementById("status-dialog-user").textContent = `${displayName(user)} (${user.username})`;
    const list = document.getElementById("status-list");
    list.replaceChildren();
    userStatuses.forEach(status => {
        const label = document.createElement("label");
        label.className = "status-option";
        const radio = document.createElement("input");
        radio.type = "radio";
        radio.name = "status";
        radio.value = status.value;
        radio.checked = status.value === user.status;
        radio.addEventListener("change", () => {
            pendingStatusChange.selectedStatus = status.value;
            document.getElementById("submit-status").disabled = status.value === user.status;
        });
        const text = document.createElement("span");
        text.className = "status-option-text";
        text.append(document.createTextNode(status.label));
        const value = document.createElement("span");
        value.className = "status-value";
        value.textContent = status.value;
        text.append(value);
        if (status.description) {
            const description = document.createElement("span");
            description.className = "status-description";
            description.textContent = status.description;
            text.append(description);
        }
        label.append(radio, text);
        list.append(label);
    });
    document.getElementById("submit-status").disabled = true;
    document.getElementById("status-dialog").showModal();
}
function closeStatusDialog() {
    pendingStatusChange = null;
    document.getElementById("status-dialog").close();
}
async function saveStatus(event) {
    event.preventDefault();
    if (!pendingStatusChange || pendingStatusChange.selectedStatus === pendingStatusChange.user.status) return;
    const {user, selectedStatus} = pendingStatusChange;
    await run(async () => {
        await apiFetch(`/api/users/${user.id}/status`, {method:"PATCH", body:JSON.stringify({status:selectedStatus})});
        closeStatusDialog();
        await loadUsers();
        showMessage("Статус користувача оновлено", "success");
    });
}
async function mutateUser(id, action, body) { await run(async () => { await apiFetch(`/api/users/${id}/${action}`, {method:"PATCH", body:JSON.stringify(body)}); await loadUsers(); showMessage("Користувача оновлено", "success"); }); }
function addRole(user) { openRoleDialog(user, "ADD"); }
function removeRole(user) { openRoleDialog(user, "REMOVE"); }
function openRoleDialog(user, mode) {
    pendingRoleAction = {user, mode};
    document.getElementById("role-dialog-title").textContent = mode === "ADD" ? "Додати роль" : "Видалити роль";
    document.getElementById("role-dialog-user").textContent = `${displayName(user)} (${user.username})`;
    const search = document.getElementById("role-search");
    search.value = "";
    const submit = document.getElementById("submit-role");
    submit.textContent = mode === "ADD" ? "Додати" : "Видалити роль";
    renderRoleOptions();
    document.getElementById("role-dialog").showModal();
    search.focus();
}
function renderRoleOptions() {
    if (!pendingRoleAction) return;
    const {user, mode} = pendingRoleAction;
    const assigned = new Set(user.roles);
    const choices = mode === "ADD"
        ? availableRoles.filter(role => !assigned.has(role))
        : [...assigned].filter(role => role !== "ADMIN" || user.id !== currentUser?.id).sort();
    const query = document.getElementById("role-search").value.trim().toLocaleLowerCase();
    const filtered = choices.filter(role => role.toLocaleLowerCase().includes(query));
    const list = document.getElementById("role-list");
    list.replaceChildren();
    document.getElementById("submit-role").disabled = true;
    if (!filtered.length) {
        const empty = document.createElement("div");
        empty.className = "role-empty";
        empty.textContent = query ? "Ролей не знайдено" : mode === "ADD" ? "Усі доступні ролі вже призначені" : "У користувача немає ролей для видалення";
        list.append(empty);
        return;
    }
    filtered.forEach(role => {
        const label = document.createElement("label");
        label.className = "role-option";
        const radio = document.createElement("input");
        radio.type = "radio";
        radio.name = "role";
        radio.value = role;
        radio.addEventListener("change", () => { document.getElementById("submit-role").disabled = false; });
        label.append(radio, document.createTextNode(role));
        list.append(label);
    });
}
function closeRoleDialog() {
    pendingRoleAction = null;
    document.getElementById("role-dialog").close();
}
async function saveRole(event) {
    event.preventDefault();
    if (!pendingRoleAction) return;
    const role = new FormData(event.currentTarget).get("role");
    if (!role) return;
    const {user, mode} = pendingRoleAction;
    await run(async () => {
        const url = `/api/users/${user.id}/roles${mode === "REMOVE" ? `/${encodeURIComponent(role)}` : ""}`;
        const options = mode === "ADD" ? {method:"POST", body:JSON.stringify({role})} : {method:"DELETE"};
        await apiFetch(url, options);
        closeRoleDialog();
        await loadUsers();
        showMessage(`${mode === "ADD" ? "Роль додано" : "Роль видалено"}. Зміни ролей набудуть чинності після повторного входу користувача.`, "success");
    });
}

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
function unitOption(unit) { return {value:unit.id, label:`${"— ".repeat(unit.depth)}${unit.name}`, searchText:[unit.name, unit.code, unit.type].filter(Boolean).join(" ")}; }
function userOption(user) {
    const detail = [user.position, user.organizationUnitName].filter(Boolean).join(" · ");
    return {value:user.id, label:`${displayName(user)} (${user.username})`, detail, searchText:[user.firstName, user.lastName, user.username, user.email, user.resourceNumber, user.position, user.organizationUnitName].filter(Boolean).join(" ")};
}
function unitName(id) { return id == null ? "—" : units.find(unit => unit.id === id)?.name || "—"; }
function unitParentPath(unit) {
    const names = [];
    const visited = new Set();
    let parentId = unit.parentId;
    while (parentId != null && !visited.has(parentId)) {
        visited.add(parentId);
        const parent = units.find(candidate => candidate.id === parentId);
        if (!parent) break;
        names.unshift(parent.name);
        parentId = parent.parentId;
    }
    return names.join(" ");
}
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

run(async () => { await loadCurrentUser(); await loadRoles(); await loadUnits(); await loadUsers(); });
