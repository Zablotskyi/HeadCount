import {apiFetch, showMessage} from "/js/api.js";

let units = [];
let selectedUnitId = null;
let pendingUnitId = null;

const form = document.getElementById("registration-form");
const unitDialog = document.getElementById("unit-dialog");
const unitSearch = document.getElementById("unit-search");
const unitSave = document.getElementById("unit-save");

document.getElementById("choose-unit").addEventListener("click", openUnitDialog);
document.getElementById("unit-cancel").addEventListener("click", () => unitDialog.close());
unitSave.addEventListener("click", saveUnitSelection);
unitSearch.addEventListener("input", renderUnits);
form.addEventListener("submit", submitRegistration);

async function loadUnits() {
    units = await apiFetch("/api/registration/organization-units");
}

function openUnitDialog() {
    pendingUnitId = selectedUnitId;
    unitSearch.value = "";
    renderUnits();
    unitDialog.showModal();
    unitSearch.focus();
}

function renderUnits() {
    const tokens = unitSearch.value.trim().toLocaleLowerCase().split(/\s+/).filter(Boolean);
    const filtered = units.filter(unit => {
        const text = [unit.name, unit.code, unit.type].filter(Boolean).join(" ").toLocaleLowerCase();
        return tokens.every(token => text.includes(token));
    });
    const list = document.getElementById("unit-list");
    list.replaceChildren();
    for (const unit of filtered) {
        const row = document.createElement("label");
        row.className = "unit-row";
        const radio = document.createElement("input");
        radio.type = "radio";
        radio.name = "registration-unit";
        radio.value = unit.id;
        radio.checked = unit.id === pendingUnitId;
        radio.addEventListener("change", () => {
            pendingUnitId = unit.id;
            unitSave.disabled = false;
        });
        const text = document.createElement("span");
        text.textContent = `${"— ".repeat(unit.depth)}${unit.name} (${unit.code} · ${unit.type})`;
        row.append(radio, text);
        list.append(row);
    }
    document.getElementById("unit-empty").hidden = filtered.length !== 0;
    unitSave.disabled = pendingUnitId == null;
}

function saveUnitSelection() {
    const unit = units.find(candidate => candidate.id === pendingUnitId);
    if (!unit) return;
    selectedUnitId = unit.id;
    document.getElementById("organization-unit-id").value = String(unit.id);
    document.getElementById("choose-unit").textContent = `${"— ".repeat(unit.depth)}${unit.name}`;
    unitDialog.close();
}

async function submitRegistration(event) {
    event.preventDefault();
    const password = document.getElementById("password");
    const confirmation = document.getElementById("password-confirmation");
    confirmation.setCustomValidity(password.value === confirmation.value ? "" : "Паролі не збігаються");
    if (!form.reportValidity()) return;
    if (selectedUnitId == null) {
        showMessage("Виберіть організаційний підрозділ", "error");
        return;
    }

    const data = new FormData(form);
    const payload = Object.fromEntries(data.entries());
    payload.organizationUnitId = selectedUnitId;
    payload.timeZone = "Europe/Kyiv";
    const submit = document.getElementById("register-submit");
    submit.disabled = true;
    try {
        await apiFetch("/api/registration", {method: "POST", body: JSON.stringify(payload)});
        form.reset();
        form.hidden = true;
        document.getElementById("message-container").hidden = true;
        document.getElementById("registration-success").hidden = false;
    } catch (error) {
        showMessage(error.message, "error");
    } finally {
        submit.disabled = false;
    }
}

try {
    await loadUnits();
} catch (error) {
    showMessage(error.message, "error");
    document.getElementById("choose-unit").disabled = true;
}
