const form = document.getElementById("login-form");
const submit = document.getElementById("login-submit");
const message = document.getElementById("login-message");
const parameters = new URLSearchParams(window.location.search);

if (parameters.has("logout")) showMessage("Ви успішно вийшли із системи.", "success");
else if (parameters.has("error")) showMessage("Невірний username або пароль.", "error");

try {
    const response = await fetch("/api/csrf", {credentials:"same-origin", headers:{Accept:"application/json"}});
    if (!response.ok) throw new Error();
    const csrf = await response.json();
    const input = document.createElement("input");
    input.type = "hidden";
    input.name = csrf.parameterName;
    input.value = csrf.token;
    form.append(input);
    submit.disabled = false;
} catch {
    showMessage("Не вдалося підготувати форму входу. Оновіть сторінку.", "error");
}

function showMessage(text, type) {
    message.textContent = text;
    message.className = `message ${type}`;
    message.hidden = false;
}
