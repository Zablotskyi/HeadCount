const allowedAdmin = 'admin@company.com';
const savedEmail = sessionStorage.getItem('userEmail');

if (savedEmail === allowedAdmin) {
    document.getElementById('admin-body').style.display = 'flex';
} else {
    alert('Доступ заборонено. Ви не адміністратор.');
    window.location.href = 'index.html';
}

let departments = JSON.parse(localStorage.getItem('departments')) || {};

function saveDepartments() {
    localStorage.setItem('departments', JSON.stringify(departments));
}

function refreshUI() {
    const deptSelect = document.getElementById('emp-dept');
    const deptList = document.getElementById('dept-list');
    const empList = document.getElementById('emp-list');

    deptSelect.innerHTML = '';
    deptList.innerHTML = '';
    empList.innerHTML = '';

    for (const key in departments) {
        const dept = departments[key];

        // Dropdown
        const opt = document.createElement('option');
        opt.value = key;
        opt.textContent = dept.name;
        deptSelect.appendChild(opt);

        // Delete department
        const li = document.createElement('li');
        li.innerHTML = `${dept.name} <button onclick="deleteDepartment('${key}')">Видалити</button>`;
        deptList.appendChild(li);

        // Manager
        const m = dept.manager;
        const managerLi = document.createElement('li');
        managerLi.innerHTML = `👔 ${m.name} (${m.email}) ${m.phone ? `📞 ${m.phone}` : ""}
      <button onclick="deleteEmployee('${key}', '${m.email}', true)">Видалити</button>`;
        empList.appendChild(managerLi);

        // Employees
        dept.employees.forEach(emp => {
            const empLi = document.createElement('li');
            empLi.innerHTML = `👤 ${emp.name} (${emp.email}) ${emp.phone ? `📞 ${emp.phone}` : ""}
        <button onclick="deleteEmployee('${key}', '${emp.email}', false)">Видалити</button>`;
            empList.appendChild(empLi);
        });
    }
}

function deleteDepartment(key) {
    if (confirm(`Ви точно хочете видалити відділ "${departments[key].name}"?`)) {
        delete departments[key];
        saveDepartments();
        refreshUI();
    }
}

function deleteEmployee(deptKey, email, isManager) {
    if (isManager) {
        if (confirm('Це керівник. Видалити весь відділ?')) {
            delete departments[deptKey];
        }
    } else {
        departments[deptKey].employees = departments[deptKey].employees.filter(e => e.email !== email);
    }
    saveDepartments();
    refreshUI();
}

document.getElementById('add-dept-form').onsubmit = e => {
    e.preventDefault();
    const key = document.getElementById('dept-key').value.trim();
    const name = document.getElementById('dept-name').value.trim();
    const mgrName = document.getElementById('manager-name').value.trim();
    const mgrEmail = document.getElementById('manager-email').value.trim();
    const mgrPhone = document.getElementById('manager-phone').value.trim();

    if (departments[key]) {
        alert('Відділ з таким ідентифікатором уже існує!');
        return;
    }

    departments[key] = {
        name,
        manager: { name: mgrName, email: mgrEmail, phone: mgrPhone },
        employees: []
    };

    saveDepartments();
    refreshUI();
    e.target.reset();
};

document.getElementById('add-emp-form').onsubmit = e => {
    e.preventDefault();
    const dept = document.getElementById('emp-dept').value;
    const name = document.getElementById('emp-name').value.trim();
    const email = document.getElementById('emp-email').value.trim();
    const phone = document.getElementById('emp-phone').value.trim();

    if (!departments[dept]) {
        alert('Відділ не знайдено!');
        return;
    }

    if (departments[dept].employees.some(e => e.email === email) ||
        departments[dept].manager.email === email) {
        alert('Такий email уже існує в цьому відділі!');
        return;
    }

    departments[dept].employees.push({ name, email, phone });
    saveDepartments();
    refreshUI();
    e.target.reset();
};

window.onload = refreshUI;
