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

    deptSelect.innerHTML = '';
    deleteDeptSelect.innerHTML = '';
    deptViewList.innerHTML = '';
    empList.innerHTML = '';
    searchResults.innerHTML = '';
    editResults.innerHTML = '';
    selectedForDeletion = null;
    selectedForEdit = null;
    document.getElementById('edit-form').style.display = 'none';

    for (const key in departments) {
        const dept = departments[key];

        // Dropdowns
        const opt = new Option(dept.name, key);
        deptSelect.appendChild(opt);

        const delOpt = new Option(dept.name, key);
        deleteDeptSelect.appendChild(delOpt);

        const editOpt = new Option(dept.name, key);
        document.getElementById('edit-dept').appendChild(new Option(dept.name, key));

        // Фільтр
        const viewBtn = document.createElement('button');
        viewBtn.className = 'dept-filter-btn';
        viewBtn.textContent = dept.name;
        viewBtn.onclick = () => toggleDeptFilter(key, viewBtn);
        if (activeDeptFilters.includes(key)) {
            viewBtn.classList.add('active');
        }
        deptViewList.appendChild(viewBtn);
    }

    // Вивід співробітників
    for (const key in departments) {
        if (activeDeptFilters.length && !activeDeptFilters.includes(key)) continue;
        const dept = departments[key];

        const m = dept.manager;
        empList.innerHTML += `
      <li>
        👔 <span style="display:inline-block; width:180px;">${m.name}</span>
        <span style="display:inline-block; width:160px;">${dept.name}</span>
        <span style="display:inline-block; width:240px;">${m.email}</span>
        <span style="display:inline-block; width:160px;">${m.phone || ""}</span>
      </li>`;

        dept.employees.forEach(emp => {
            empList.innerHTML += `
        <li>
          👤 <span style="display:inline-block; width:180px;">${emp.name}</span>
          <span style="display:inline-block; width:160px;">${dept.name}</span>
          <span style="display:inline-block; width:240px;">${emp.email}</span>
          <span style="display:inline-block; width:160px;">${emp.phone || ""}</span>
        </li>`;
        });
    }

    // Пошук для видалення
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
                    li.textContent = `👔 ${m.name} (${m.email}) — ${dept.name}`;
                    li.onclick = () => selectUserForDeletion(key, m.email, true, li);
                    searchResults.appendChild(li);
                }
                dept.employees.forEach(e => {
                    if (e.name.toLowerCase().includes(val) || e.email.toLowerCase().includes(val)) {
                        const li = document.createElement('li');
                        li.textContent = `👤 ${e.name} (${e.email}) — ${dept.name}`;
                        li.onclick = () => selectUserForDeletion(key, e.email, false, li);
                        searchResults.appendChild(li);
                    }
                });
            }
        };
    }

    // Пошук для редагування
    if (editSearch) {
        editSearch.oninput = () => {
            const val = editSearch.value.toLowerCase().trim();
            editResults.innerHTML = '';
            selectedForEdit = null;
            document.getElementById('edit-form').style.display = 'none';

            if (!val) return;

            for (const key in departments) {
                const dept = departments[key];
                const m = dept.manager;
                if (m.name.toLowerCase().includes(val) || m.email.toLowerCase().includes(val)) {
                    const li = document.createElement('li');
                    li.textContent = `👔 ${m.name} (${m.email}) — ${dept.name}`;
                    li.onclick = () => selectUserForEdit(key, m.email, true, m);
                    editResults.appendChild(li);
                }
                dept.employees.forEach(e => {
                    if (e.name.toLowerCase().includes(val) || e.email.toLowerCase().includes(val)) {
                        const li = document.createElement('li');
                        li.textContent = `👤 ${e.name} (${e.email}) — ${dept.name}`;
                        li.onclick = () => selectUserForEdit(key, e.email, false, e);
                        editResults.appendChild(li);
                    }
                });
            }
        };
    }
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

    // Очистити поле пошуку
    document.getElementById('search-emp').value = '';
    document.getElementById('search-results').innerHTML = '';
    selectedForDeletion = null;
}

function selectUserForEdit(deptKey, email, isManager, data) {
    selectedForEdit = { deptKey, email, isManager };
    document.getElementById('edit-name').value = data.name;
    document.getElementById('edit-email').value = data.email;
    document.getElementById('edit-phone').value = data.phone || '';
    document.getElementById('edit-dept').value = deptKey;
    document.getElementById('edit-form').style.display = 'flex';
}

document.getElementById('edit-form').onsubmit = e => {
    e.preventDefault();
    if (!selectedForEdit) {
        alert("Оберіть співробітника для редагування.");
        return;
    }

    const oldDeptKey = selectedForEdit.deptKey;
    const oldEmail = selectedForEdit.email;
    const isManager = selectedForEdit.isManager;

    const newName = document.getElementById('edit-name').value.trim();
    const newEmail = document.getElementById('edit-email').value.trim();
    const newPhone = document.getElementById('edit-phone').value.trim();
    const newDeptKey = document.getElementById('edit-dept').value;

    if (isManager) {
        departments[oldDeptKey].manager = { name: newName, email: newEmail, phone: newPhone };
        if (oldDeptKey !== newDeptKey) {
            departments[newDeptKey] = {
                name: departments[oldDeptKey].name,
                manager: departments[oldDeptKey].manager,
                employees: departments[oldDeptKey].employees
            };
            delete departments[oldDeptKey];
        }
    } else {
        const emp = departments[oldDeptKey].employees.find(e => e.email === oldEmail);
        if (!emp) return;

        if (oldDeptKey === newDeptKey) {
            emp.name = newName;
            emp.email = newEmail;
            emp.phone = newPhone;
        } else {
            departments[oldDeptKey].employees = departments[oldDeptKey].employees.filter(e => e.email !== oldEmail);
            departments[newDeptKey].employees.push({ name: newName, email: newEmail, phone: newPhone });
        }
    }

    saveDepartments();
    refreshUI();

    // Очистити редагування
    document.getElementById('edit-form').style.display = 'none';
    document.getElementById('edit-search').value = '';
    document.getElementById('edit-results').innerHTML = '';
    selectedForEdit = null;
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
