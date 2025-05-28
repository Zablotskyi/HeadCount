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
let selectedForEdit = null;
let activeDeptFilters = [];

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
    const editSearch = document.getElementById('edit-search');
    const editResults = document.getElementById('edit-results');
    const editForm = document.getElementById('edit-form');
    const editDeptSelect = document.getElementById('edit-dept');

    deptSelect.innerHTML = '';
    deleteDeptSelect.innerHTML = '';
    deptViewList.innerHTML = '';
    empList.innerHTML = '';
    searchResults.innerHTML = '';
    editResults.innerHTML = '';
    editDeptSelect.innerHTML = '';
    selectedForDeletion = null;
    selectedForEdit = null;
    editForm.style.display = 'none';

    for (const key in departments) {
        const dept = departments[key];

        // Dropdowns
        deptSelect.appendChild(new Option(dept.name, key));
        deleteDeptSelect.appendChild(new Option(dept.name, key));
        editDeptSelect.appendChild(new Option(dept.name, key));

        // View-only department grid (кнопки фільтрів)
        const viewBtn = document.createElement('button');
        viewBtn.className = 'dept-filter-btn';
        viewBtn.textContent = dept.name;
        viewBtn.onclick = () => toggleDeptFilter(key, viewBtn);
        if (activeDeptFilters.includes(key)) viewBtn.classList.add('active');
        deptViewList.appendChild(viewBtn);
    }

    // Список співробітників (з урахуванням фільтра)
    for (const key in departments) {
        if (activeDeptFilters.length && !activeDeptFilters.includes(key)) continue;
        const dept = departments[key];

        // Manager
        const m = dept.manager;
        empList.appendChild(createUserRow(m, dept.name, true));

        // Employees
        dept.employees.forEach(emp => {
            empList.appendChild(createUserRow(emp, dept.name, false));
        });
    }

    // Пошук видалення
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

    // Пошук редагування
    if (editSearch) {
        editSearch.oninput = () => {
            const val = editSearch.value.toLowerCase().trim();
            editResults.innerHTML = '';
            selectedForEdit = null;
            editForm.style.display = 'none';

            if (!val) return;

            for (const key in departments) {
                const dept = departments[key];
                const m = dept.manager;
                if (m.name.toLowerCase().includes(val) || m.email.toLowerCase().includes(val)) {
                    const li = document.createElement('li');
                    li.innerHTML = `👔 ${m.name} (${m.email}) — ${dept.name}`;
                    li.onclick = () => populateEditForm(key, m.email, true, m);
                    editResults.appendChild(li);
                }

                dept.employees.forEach(e => {
                    if (e.name.toLowerCase().includes(val) || e.email.toLowerCase().includes(val)) {
                        const li = document.createElement('li');
                        li.innerHTML = `👤 ${e.name} (${e.email}) — ${dept.name}`;
                        li.onclick = () => populateEditForm(key, e.email, false, e);
                        editResults.appendChild(li);
                    }
                });
            }
        };
    }
}

function createUserRow(user, deptName, isManager) {
    const li = document.createElement('li');
    li.innerHTML = `
        ${isManager ? '👔' : '👤'} 
        <span style="display:inline-block; width:180px;">${user.name}</span>
        <span style="display:inline-block; width:160px;">${deptName}</span>
        <span style="display:inline-block; width:240px;">${user.email}</span>
        <span style="display:inline-block; width:160px;">${user.phone || ""}</span>`;
    return li;
}

function toggleDeptFilter(key, button) {
    if (activeDeptFilters.includes(key)) {
        activeDeptFilters = activeDeptFilters.filter(k => k !== key);
        button.classList.remove('active');
    } else {
        activeDeptFilters.push(key);
        button.classList.add('active');
    }
    refreshUI();
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

function populateEditForm(deptKey, email, isManager, userObj) {
    selectedForEdit = { deptKey, email, isManager, userObj };
    document.getElementById('edit-name').value = userObj.name;
    document.getElementById('edit-email').value = userObj.email;
    document.getElementById('edit-phone').value = userObj.phone || "";
    document.getElementById('edit-dept').value = deptKey;
    document.getElementById('edit-form').style.display = 'flex';
    document.getElementById('edit-search').value = `${userObj.name} (${userObj.email})`;
}

function deleteSelectedDepartment() {
    const select = document.getElementById('delete-dept-select');
    const key = select.value;
    if (!key) return;

    if (confirm(`Ви точно хочете видалити відділ "${departments[key].name}"?`)) {
        delete departments[key];
        activeDeptFilters = activeDeptFilters.filter(k => k !== key);
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
            activeDeptFilters = activeDeptFilters.filter(k => k !== deptKey);
        }
    } else {
        departments[deptKey].employees = departments[deptKey].employees.filter(e => e.email !== email);
    }

    saveDepartments();
    refreshUI();
}

document.getElementById('edit-form').onsubmit = e => {
    e.preventDefault();
    if (!selectedForEdit) return;

    const name = document.getElementById('edit-name').value.trim();
    const email = document.getElementById('edit-email').value.trim();
    const phone = document.getElementById('edit-phone').value.trim();
    const newDeptKey = document.getElementById('edit-dept').value;

    const { deptKey, isManager, userObj } = selectedForEdit;

    if (isManager) {
        departments[deptKey].manager = { name, email, phone };
        if (deptKey !== newDeptKey) {
            departments[newDeptKey].manager = { name, email, phone };
            delete departments[deptKey];
        }
    } else {
        departments[deptKey].employees = departments[deptKey].employees.filter(e => e.email !== userObj.email);
        departments[newDeptKey].employees.push({ name, email, phone });
    }

    saveDepartments();
    refreshUI();
    e.target.reset();
};

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
