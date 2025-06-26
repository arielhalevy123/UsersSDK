const apiBase = 'http://localhost:8080/api';

async function registerAdmin() {
    const name = document.getElementById('registerName').value;
    const email = document.getElementById('registerEmail').value;
    const password = document.getElementById('registerPassword').value;
    const role = document.getElementById('registerRole').value;
    const adminId = document.getElementById('adminId').value;
    const payload = { name, email, password, role };
    if (role === 'USER' && adminId) {
        payload.adminId = parseInt(adminId);
    }

    payload.customFields = collectCustomFields();

    const res = await fetch(`${apiBase}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    const data = await res.json().catch(() => ({}));

    if (res.ok) {
        alert('Registered successfully');
    } else {
        alert('Error: ' + (data.message || JSON.stringify(data)));
    }

    clearRegisterForm();
}

function clearRegisterForm() {
    document.getElementById('registerName').value = '';
    document.getElementById('registerEmail').value = '';
    document.getElementById('registerPassword').value = '';
    document.getElementById('adminId').value = '';
    document.getElementById('registerRole').value = 'ADMIN';
    document.getElementById('customFieldsContainer').innerHTML = '<h3>Custom Fields</h3>';
}

function addCustomField() {
    const container = document.getElementById('customFieldsContainer');
    const pair = document.createElement('div');
    pair.classList.add('custom-field-pair');
    pair.innerHTML = `
        <input type="text" class="field-name" placeholder="Field Name">
        <input type="text" class="field-value" placeholder="Field Value">
    `;
    container.appendChild(pair);
}

function collectCustomFields() {
    const fields = [];
    document.querySelectorAll('.custom-field-pair').forEach(pair => {
        const fieldName = pair.querySelector('.field-name').value;
        const fieldValue = pair.querySelector('.field-value').value;
        if (fieldName && fieldValue) {
            fields.push({ fieldName, fieldValue });
        }
    });
    return fields;
}

async function loginAdmin() {
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    const res = await fetch(`${apiBase}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
    });

    const data = await res.json();
    if (res.ok && data.token) {
        localStorage.setItem('jwt', data.token);
        getAdminInfo();
    } else {
        alert('Login failed');
    }

    document.getElementById('loginEmail').value = '';
    document.getElementById('loginPassword').value = '';
}

async function getAdminInfo() {
    const token = localStorage.getItem('jwt');
    const res = await fetch(`${apiBase}/auth/me`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });

    if (!res.ok) {
        alert('Error loading admin info');
        return;
    }

    const data = await res.json();
    document.getElementById('adminName').textContent = data.name;
    document.getElementById('adminEmail').textContent = data.email;
    document.getElementById('adminRole').textContent = data.role;
    document.getElementById('adminInfo').classList.remove('hidden');
    document.getElementById('registerSection').classList.add('hidden');
    document.getElementById('loginSection').classList.add('hidden');

    if (data.role === 'ADMIN') {
        loadUsersManagedByAdmin();
    }
}

async function loadUsersManagedByAdmin() {
    const token = localStorage.getItem('jwt');
    const res = await fetch(`${apiBase}/auth/my-users`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    const users = await res.json();

    const tableBody = document.querySelector('#userTable tbody');
    tableBody.innerHTML = '';

    users.forEach(user => {
        const row = document.createElement('tr');
        const customFields = user.customFields.map(f => `${f.fieldName}: ${f.fieldValue}`).join('<br>');

        row.innerHTML = `
        <td>${user.name}</td>
        <td>${user.email}</td>
        <td>${user.role}</td>
        <td>${customFields}</td>
        <td><button onclick='openEditForm(${JSON.stringify(user)})'>Edit</button></td>
    `;
        tableBody.appendChild(row);
    });

    document.getElementById('userListSection').classList.remove('hidden');
}
let editingUserId = null;

function openEditForm(user) {
    editingUserId = user.id;
    document.getElementById('editName').value = user.name;
    document.getElementById('editEmail').value = user.email;

    const container = document.getElementById('editCustomFieldsContainer');
    container.innerHTML = '';
    (user.customFields || []).forEach(field => {
        const pair = document.createElement('div');
        pair.classList.add('custom-field-pair');
        pair.innerHTML = `
            <input type="text" class="field-name" value="${field.fieldName}" placeholder="Field Name">
            <input type="text" class="field-value" value="${field.fieldValue}" placeholder="Field Value">
        `;
        container.appendChild(pair);
    });

    document.getElementById('editUserSection').classList.remove('hidden');
    document.getElementById('editUserSection').style.display = 'block';
}

function addEditCustomField() {
    const container = document.getElementById('editCustomFieldsContainer');
    const pair = document.createElement('div');
    pair.classList.add('custom-field-pair');
    pair.innerHTML = `
        <input type="text" class="field-name" placeholder="Field Name">
        <input type="text" class="field-value" placeholder="Field Value">
    `;
    container.appendChild(pair);
}

async function saveUserEdits(userId) {
    const token = localStorage.getItem("jwt");

    const updatedUser = {
        name: document.getElementById("editName").value,
        email: document.getElementById("editEmail").value,
        customFields: collectCustomFieldsFromEditForm()
    };

    const response = await fetch(`http://localhost:8080/api/auth/users/${userId}`, {
        method: "PUT",
        headers: {
            "Content-Type": "application/json",
            "Authorization": "Bearer " + token
        },
        body: JSON.stringify(updatedUser)
    });

    if (!response.ok) {
        const errorText = await response.text();
        console.error("Failed to update user:", errorText);
        alert("עדכון המשתמש נכשל");
        return;
    }

    const updatedData = await response.json();
    console.log("User updated successfully:", updatedData);
    alert("המשתמש עודכן בהצלחה");

    // 👇 במקום location.reload:
    document.getElementById("editUserSection").classList.add("hidden");
    await loadUsersManagedByAdmin();  // טען מחדש רק את רשימת המשתמשים
}
function collectCustomFieldsFromEditForm() {
    const fields = [];
    document.querySelectorAll('#editCustomFieldsContainer .custom-field-pair').forEach(pair => {
        const fieldName = pair.querySelector('.field-name').value;
        const fieldValue = pair.querySelector('.field-value').value;
        if (fieldName && fieldValue) {
            fields.push({ fieldName, fieldValue });
        }
    });
    return fields;
}

function logout() {
    localStorage.removeItem('jwt');
    location.reload();
}