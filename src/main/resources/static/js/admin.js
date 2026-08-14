import {apiFetch, logout, showMessage} from "/js/api.js";

let units = [];
let users = [];
document.getElementById("logout").addEventListener("click", logout);
document.getElementById("all-users").addEventListener("click", loadUsers);
document.getElementById("search-form").addEventListener("submit", async event => {
    event.preventDefault(); await run(() => loadUsers(new FormData(event.currentTarget).get("q")));
});
document.getElementById("unit-form").addEventListener("submit", createUnit);
document.getElementById("user-form").addEventListener("submit", createUser);

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
    const body = document.getElementById("unit-table"); body.replaceChildren();
    units.forEach(unit => {
        const row = body.insertRow();
        appendCells(row, [`${"— ".repeat(unit.depth)}${unit.name}`, unit.type, unit.parentId || "—", unit.managerId || "—", unit.active ? "Так" : "Ні"]);
        const actions = row.insertCell(); actions.className = "actions";
        addButton(actions, "Parent", () => changeParent(unit)); addButton(actions, "Manager", () => assignManager(unit));
        addButton(actions, unit.active ? "Деактивувати" : "Активувати", () => mutateUnit(unit.id, "active", {active: !unit.active}));
    });
}

async function loadUsers(query) {
    users = await apiFetch(query?.trim() ? `/api/users/search?q=${encodeURIComponent(query.trim())}` : "/api/users");
    const body = document.getElementById("user-table"); body.replaceChildren();
    users.forEach(user => {
        const row = body.insertRow(); appendCells(row, [`${user.firstName} ${user.lastName} (${user.username})`, user.organizationUnitName || "—", `${user.status} / ${user.enabled ? "active" : "disabled"}`, [...user.roles].join(", ")]);
        const actions = row.insertCell(); actions.className = "actions";
        addButton(actions, "Профіль", () => editProfile(user)); addButton(actions, "Unit", () => assignUnit(user)); addButton(actions, "Line manager", () => assignLineManager(user));
        addButton(actions, "Статус", () => changeStatus(user)); addButton(actions, user.enabled ? "Деактивувати" : "Активувати", () => mutateUser(user.id, "active", {active: !user.enabled}));
        addButton(actions, "+ роль", () => addRole(user)); addButton(actions, "− роль", () => removeRole(user));
    });
}

async function createUnit(event) {
    event.preventDefault(); const data = Object.fromEntries(new FormData(event.currentTarget));
    await run(async () => { await apiFetch("/api/organization-units", {method:"POST", body:JSON.stringify({...data, parentId:numberOrNull(data.parentId), sortOrder:Number(data.sortOrder)})}); event.currentTarget.reset(); await loadUnits(); showMessage("Unit створено", "success"); });
}

async function createUser(event) {
    event.preventDefault(); const data = Object.fromEntries(new FormData(event.currentTarget));
    await run(async () => { await apiFetch("/api/users", {method:"POST", body:JSON.stringify({...data, organizationUnitId:numberOrNull(data.organizationUnitId)})}); event.currentTarget.reset(); await loadUsers(); showMessage("Користувача створено", "success"); });
}

async function changeParent(unit) { const value = window.prompt("Новий parent ID (порожньо = root):", unit.parentId || ""); if (value === null) return; await mutateUnit(unit.id, "parent", {parentId:numberOrNull(value)}); }
async function assignManager(unit) { const value = window.prompt("Manager user ID (порожньо = прибрати):", unit.managerId || ""); if (value === null) return; await mutateUnit(unit.id, "manager", {managerId:numberOrNull(value)}); }
async function mutateUnit(id, action, body) { await run(async () => { await apiFetch(`/api/organization-units/${id}/${action}`, {method:"PATCH", body:JSON.stringify(body)}); await loadUnits(); showMessage("Unit оновлено", "success"); }); }

async function editProfile(user) {
    const firstName = window.prompt("Ім’я:", user.firstName); if (firstName === null) return;
    const lastName = window.prompt("Прізвище:", user.lastName); if (lastName === null) return;
    const email = window.prompt("Email:", user.email); if (email === null) return;
    const body = {username:user.username, resourceNumber:user.resourceNumber, grade:user.grade, firstName, lastName, mobileNumber:user.mobileNumber, email, country:user.country, city:user.city, office:user.office, position:user.position, address:user.address, authorizedPersonPhoneNumber:user.authorizedPersonPhoneNumber, timeZone:user.timeZone};
    await run(async () => { await apiFetch(`/api/users/${user.id}`, {method:"PUT", body:JSON.stringify(body)}); await loadUsers(); showMessage("Профіль оновлено", "success"); });
}
async function assignUnit(user) { const value = window.prompt("Organization unit ID (порожньо = прибрати):", user.organizationUnitId || ""); if (value !== null) await mutateUser(user.id, "organization-unit", {organizationUnitId:numberOrNull(value)}); }
async function assignLineManager(user) { const value = window.prompt("Line manager user ID (порожньо = прибрати):", user.lineManagerId || ""); if (value !== null) await mutateUser(user.id, "line-manager", {lineManagerId:numberOrNull(value)}); }
async function changeStatus(user) { const value = window.prompt("Status: PENDING_EMAIL_VERIFICATION, PENDING_APPROVAL, ACTIVE, REJECTED, SUSPENDED, ARCHIVED", user.status); if (value) await mutateUser(user.id, "status", {status:value.trim().toUpperCase()}); }
async function mutateUser(id, action, body) { await run(async () => { await apiFetch(`/api/users/${id}/${action}`, {method:"PATCH", body:JSON.stringify(body)}); await loadUsers(); showMessage("Користувача оновлено", "success"); }); }
async function addRole(user) { const role = window.prompt("Role:"); if (role) await run(async () => { await apiFetch(`/api/users/${user.id}/roles`, {method:"POST", body:JSON.stringify({role:role.trim().toUpperCase()})}); await loadUsers(); }); }
async function removeRole(user) { const role = window.prompt(`Role для видалення (${[...user.roles].join(", ")}):`); if (role) await run(async () => { await apiFetch(`/api/users/${user.id}/roles/${encodeURIComponent(role.trim().toUpperCase())}`, {method:"DELETE"}); await loadUsers(); }); }

function appendCells(row, values) { values.forEach(value => { const cell = row.insertCell(); cell.textContent = value ?? ""; }); }
function addButton(container, label, action) { const button = document.createElement("button"); button.type="button"; button.textContent=label; button.addEventListener("click", action); container.append(button); }
function numberOrNull(value) { return value === "" || value == null ? null : Number(value); }
async function run(action) { try { await action(); } catch (error) { showMessage(error.message, "error"); } }

run(async () => { await loadUnits(); await loadUsers(); });
