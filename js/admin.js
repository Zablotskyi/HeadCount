const allowedAdmin = 'admin@company.com';
const savedEmail = sessionStorage.getItem('userEmail');

if (savedEmail === allowedAdmin) {
    document.getElementById('admin-body').style.display = 'flex';
} else {
    alert('Доступ заборонено. Ви не адміністратор.');
    window.location.href = 'index.html';
}

let departments = JSON.parse(localStorage.getItem('departments')) || {};
let selectedForDeletion = null;

function saveDepartments() {
    localStorage.setItem('departments', JSON.stringify(departments));
}

function refreshUI() {
    const deptSelect = document.getElementById('emp-dept');
    const deleteDeptSelect = document.getElementById('delete-dept-select');
    const deptViewList = document.getElementById('dept-view-list');
    const empList = document.getElementById('emp-list');
    const searchInput = document.getElementById('search-emp');
    const searchResults = document.getElementById('search-results');

    deptSelect.innerHTML = '';
    deleteDeptSelect.innerHTML = '';
    deptViewList.innerHTML = '';
    empList.innerHTML = '';
    searchResults.innerHTML = '';
    selectedForDeletion = null;

    for (const key in departments) {
        const dept = departments[key];

        // Dropdowns
        const opt = new Option(dept.name, key);
        deptSelect.appendChild(opt);

        const delOpt = new Option(dept.name, key);
        deleteDeptSelect.appendChild(delOpt);

        // View-only department grid
        const viewItem = document.createElement('div');
        viewItem.textContent = dept.name;
        deptViewList.appendChild(viewItem);

        // Manager
        const m = dept.manager;
        const managerLi = document.createElement('li');
        managerLi.innerHTML = `
      👔 
      <span style="display:inline-block; width:180px;">${m.name}</span>
      <span style="display:inline-block; width:160px;">${dept.name}</span>
      <span style="display:inline-block; width:240px;">${m.email}</span>
      <span style="display:inline-block; width:160px;">${m.phone || ""}</span>`;
        empList.appendChild(managerLi);

        // Employees
        dept.employees.forEach(emp => {
            const empLi = document.createElement('li');
            empLi.innerHTML = `
        👤 
        <span style="display:inline-block; width:180px;">${emp.name}</span>
        <span style="display:inline-block; width:160px;">${dept.name}</span>
        <span style="display:inline-block; width:240px;">${emp.email}</span>
        <span style="display:inline-block; width:160px;">${emp.phone || ""}</span>`;
            empList.appendChild(empLi);
        });
    }

    // Пошук співробітника
    if (searchInput) {
        searchInput.oninput = () => {
            const val = searchInput.value.toLowerCase().trim();
            searchResults.innerHTML = '';
            selectedForDeletion = null;

            if (!val) return;

            for (const key in departments) {
                const dept = departments[key];
                const m = dept.manager;
                if (m.name.toLowerCase().includes(val) || m.email.toLowerCase().includes(val)) {
                    const li = document.createElement('li');
                    li.innerHTML = `👔 ${m.name} (${m.email}) — ${dept.name}`;
                    li.onclick = () => selectUserForDeletion(key, m.email, true, li);
                    searchResults.appendChild(li);
                }

                dept.employees.forEach(e => {
                    if (e.name.toLowerCase().includes(val) || e.email.toLowerCase().includes(val)) {
                        const li = document.createElement('li');
                        li.innerHTML = `👤 ${e.name} (${e.email}) — ${dept.name}`;
                        li.onclick = () => selectUserForDeletion(key, e.email, false, li);
                        searchResults.appendChild(li);
                    }
                });
            }
        };
    }
}

function selectUserForDeletion(deptKey, email, isManager, liElement) {
    selectedForDeletion = { deptKey, email, isManager };

    const rawText = liElement.innerText;
    const match = rawText.match(/([^\(]+)\(([^\)]+)\)/);
    if (match) {
        const displayName = match[1].trim();
        const displayEmail = match[2].trim();
        document.getElementById('search-emp').value = `${displayName} (${displayEmail})`;
    }
}

function deleteSelectedDepartment() {
    const select = document.getElementById('delete-dept-select');
    const key = select.value;
    if (!key) return;

    if (confirm(`Ви точно хочете видалити відділ "${departments[key].name}"?`)) {
        delete departments[key];
        saveDepartments();
        refreshUI();
    }
}

function deleteSelectedEmployee() {
    if (!selectedForDeletion) {
        alert("Оберіть співробітника зі списку пошуку.");
        return;
    }

    const { deptKey, email, isManager } = selectedForDeletion;

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
