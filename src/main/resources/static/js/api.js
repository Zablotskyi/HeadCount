let csrfPromise;

export class ApiError extends Error {
    constructor(message, status, details) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.details = details;
    }
}

async function csrf() {
    csrfPromise ??= fetch("/api/csrf", {
        credentials: "same-origin",
        headers: {Accept: "application/json"}
    }).then(async response => {
        if (!response.ok) throw new ApiError("Не вдалося отримати CSRF token", response.status);
        return response.json();
    }).catch(error => {
        csrfPromise = undefined;
        throw error;
    });
    return csrfPromise;
}

export async function apiFetch(url, options = {}) {
    const request = {...options};
    const method = (request.method || "GET").toUpperCase();
    request.credentials = "same-origin";
    request.headers = new Headers(request.headers || {});
    request.headers.set("Accept", "application/json");

    if (request.body && !(request.body instanceof FormData)) {
        request.headers.set("Content-Type", "application/json");
    }
    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
        const token = await csrf();
        request.headers.set(token.headerName, token.token);
    }

    const response = await fetch(url, request);
    if (response.status === 401 || (response.redirected && response.url.includes("/login"))) {
        window.location.assign("/login");
        throw new ApiError("Потрібна автентифікація", 401);
    }
    if (response.status === 403) {
        throw new ApiError("Недостатньо прав для цієї операції", 403);
    }
    if (!response.ok) {
        const payload = await response.json().catch(() => ({}));
        const validation = payload.validationErrors
            ? Object.entries(payload.validationErrors).map(([field, message]) => `${field}: ${message}`).join("; ")
            : "";
        throw new ApiError(validation || payload.message || `HTTP ${response.status}`, response.status, payload);
    }
    if (response.status === 204) return null;
    return response.json();
}

export async function logout() {
    const token = await csrf();
    const body = new URLSearchParams();
    body.set(token.parameterName || "_csrf", token.token);
    await fetch("/logout", {
        method: "POST",
        credentials: "same-origin",
        headers: {[token.headerName]: token.token}
    });
    window.location.assign("/login?logout");
}

export function showMessage(message, type = "info", containerId = "message-container") {
    const container = document.getElementById(containerId);
    if (!container) {
        window.alert(message);
        return;
    }
    container.textContent = message;
    container.className = `message ${type}`;
    container.hidden = false;
}
