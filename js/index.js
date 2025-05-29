const SECURITY_EMAIL = "security@company.com";
const ADMIN_EMAIL = "admin@company.com";

let departments = JSON.parse(localStorage.getItem("departments")) || {};
let currentUser = null;
let activeDeptFilters = [];

function login() {
    const email = document.getElementById("email-input").value.trim().toLowerCase();
    const password = document.getElementById("password-input").value.trim();

    if (!email || !password) {
        alert("Введіть email і пароль.");
        return;
    }

    if (email !== password) {
        alert("Невірний пароль (тимчасово він має збігатись із email).");
        return;
    }

    sessionStorage.setItem("userEmail", email);

    if (email === ADMIN_EMAIL) {
        window.location.href = "admin.html";
        return;
    }

    if (email === SECURITY_EMAIL) {
        currentUser = { email, role: "security" };
    } else {
        for (const deptKey in departments) {
            const dept = departments[deptKey];
            if (dept.manager.email === email) {
                currentUser = { ...dept.manager, role: "manager", deptKey };
            } else {
                const emp = dept.employees.find((e) => e.email === email);
                if (emp) {
                    currentUser = { ...emp, role: "employee", deptKey };
                }
            }
            if (currentUser) break;
        }
    }

    if (!currentUser) {
        alert("Email не знайдено в структурі.");
        return;
    }

    document.getElementById("login-section").style.display = "none";
    document.querySelector(".tree").style.display = "flex";

    if (["security"].includes(currentUser.role)) {
        populateFilterButtons();
    } else {
        document.getElementById("department-filter-buttons").style.display = "none";
        document.getElementById("headcount-btn").style.display = "none";
    }

    buildInterface();
}

function populateFilterButtons() {
    const container = document.getElementById("department-filter-buttons");
    container.innerHTML = "";
    for (const key in departments) {
        const btn = document.createElement("button");
        btn.textContent = departments[key].name;
        btn.className = "dept-filter-btn";
        btn.onclick = () => toggleDeptFilter(key, btn);
        container.appendChild(btn);
    }
    updateFilterButtonColors();
}

function toggleDeptFilter(key, button) {
    if (activeDeptFilters.includes(key)) {
        activeDeptFilters = activeDeptFilters.filter(k => k !== key);
        button.classList.remove("active");
    } else {
        activeDeptFilters.push(key);
        button.classList.add("active");
    }
    buildInterface();
}

function updateFilterButtonColors() {
    document.querySelectorAll(".dept-filter-btn").forEach((btn, index) => {
        const deptKeys = Object.keys(departments);
        const key = deptKeys[index];
        const emails = [departments[key].manager.email, ...departments[key].employees.map(e => e.email)];
        const hasRed = emails.some(email => localStorage.getItem(email) === "red");
        const allGreen = emails.every(email => localStorage.getItem(email) === "green");

        btn.style.backgroundColor = hasRed ? "#f88" : allGreen ? "#8f8" : "";
    });
}

function buildInterface() {
    const container = document.getElementById("departments-container");
    container.innerHTML = "";

    const visibleDepartments = ["security"].includes(currentUser.role)
        ? Object.entries(departments).filter(([key]) =>
            activeDeptFilters.length === 0 || activeDeptFilters.includes(key))
        : [[currentUser.deptKey, departments[currentUser.deptKey]]];

    visibleDepartments.forEach(([deptKey, dept]) => {
        const deptDiv = document.createElement("div");
        deptDiv.className = "department";

        const title = document.createElement("h3");
        title.textContent = dept.name;
        deptDiv.appendChild(title);

        const m = dept.manager;
        deptDiv.appendChild(createNodeHTML(m.name + " (Керівник)", m.email, m.phone));

        const empContainer = document.createElement("div");
        empContainer.className = "subordinates";

        dept.employees.forEach((emp) => {
            empContainer.appendChild(createNodeHTML(emp.name, emp.email, emp.phone));
        });

        deptDiv.appendChild(empContainer);
        container.appendChild(deptDiv);
    });

    setPermissions();
    updateFilterButtonColors();
}

function createNodeHTML(name, email, phone = "") {
    const div = document.createElement("div");
    div.className = "node";
    div.id = email;
    div.onclick = () => confirmPresence(email);

    const savedColor = localStorage.getItem(email) || "green";
    const savedTime = localStorage.getItem(`time-${email}`);
    const savedBy = localStorage.getItem(`by-${email}`);

    div.style.backgroundColor = savedColor;

    let byNote = "";
    if (savedBy === "security") byNote = " — підтвердив координатор безпеки";
    else if (savedBy === "manager") byNote = " — підтвердив керівник";

    div.innerHTML = `
    <div>${name}</div>
    ${phone ? `<div class="phone-label">${phone}</div>` : ""}
    <div class="id-label">${email}</div>
    <div class="time-label" id="time-${email}">
      ${savedTime ? `Останнє підтвердження: ${savedTime}${byNote}` : ""}
    </div>
  `;
    return div;
}

function getAllUserEmails() {
    const emails = [];
    Object.values(departments).forEach((dept) => {
        emails.push(dept.manager.email);
        dept.employees.forEach((e) => emails.push(e.email));
    });
    return emails;
}

function getCurrentDeptEmails() {
    const dept = departments[currentUser.deptKey];
    return [dept.manager.email, ...dept.employees.map((e) => e.email)];
}

function setPermissions() {
    const allEmails = getAllUserEmails();
    allEmails.forEach((email) => {
        const el = document.getElementById(email);
        if (["security"].includes(currentUser.role)) return;
        if (currentUser.role === "manager") {
            if (!getCurrentDeptEmails().includes(email)) {
                el.classList.add("disabled");
            }
        } else if (email !== currentUser.email) {
            el.classList.add("disabled");
        }
    });
}

function startHeadCount() {
    if (!["security"].includes(currentUser.role)) return;

    const emails = [];

    Object.entries(departments).forEach(([key, dept]) => {
        if (activeDeptFilters.length === 0 || activeDeptFilters.includes(key)) {
            emails.push(dept.manager.email);
            dept.employees.forEach((e) => emails.push(e.email));
        }
    });

    emails.forEach((email) => {
        const el = document.getElementById(email);
        if (el) {
            el.style.backgroundColor = "red";
            localStorage.setItem(email, "red");
            localStorage.removeItem(`time-${email}`);
            localStorage.removeItem(`by-${email}`);
            const timeLabel = document.getElementById(`time-${email}`);
            if (timeLabel) timeLabel.textContent = "";
        }
    });

    updateFilterButtonColors();
}

function confirmPresence(email) {
    const isSecurity = currentUser.role === "security";
    const isManager = currentUser.role === "manager";
    const isSelf = email === currentUser.email;

    if (!(isSecurity || isManager || isSelf)) return;

    const el = document.getElementById(email);
    if (el.style.backgroundColor === "red") {
        el.style.backgroundColor = "green";
        localStorage.setItem(email, "green");

        const time = new Date().toLocaleString();
        localStorage.setItem(`time-${email}`, time);

        let by = "";
        if (isSecurity && !isSelf) by = "security";
        else if (isManager && !isSelf) by = "manager";
        localStorage.setItem(`by-${email}`, by);

        const timeLabel = document.getElementById(`time-${email}`);
        if (timeLabel) {
            let label = `Останнє підтвердження: ${time}`;
            if (by === "security") label += " — підтвердив координатор безпеки";
            else if (by === "manager") label += " — підтвердив керівник";
            timeLabel.textContent = label;
        }

        updateFilterButtonColors();
    }
}

window.onload = () => {
    const savedEmail = sessionStorage.getItem("userEmail");
    if (savedEmail) {
        document.getElementById("email-input").value = savedEmail;
        document.getElementById("password-input").value = savedEmail;
        login();
    }
};
