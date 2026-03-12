const services = [];

// Modal Adicionar Serviço
function toggleServiceModal(show) {
    const modal = document.getElementById('addServiceModal');
    modal.classList.toggle('hidden', !show);
    document.body.classList.toggle('overflow-hidden', show);

    if (show) {
        document.getElementById('description').focus();
    } else {
        services.length = 0;
        renderServices();
        document.getElementById('description').value = '';
        document.getElementById('startDateTime').value = '';
        document.getElementById('endDateTime').value = '';
    }
}

// Modal Editar Serviço
function toggleEditModal(show) {
    const modal = document.getElementById('editServiceModal');
    modal.classList.toggle('hidden', !show);
    document.body.classList.toggle('overflow-hidden', show);
}

// Modal Adicionar Folga
function toggleTimeOffModal(show) {
    const modal = document.getElementById('addTimeOffModal');
    modal.classList.toggle('hidden', !show);
    document.body.classList.toggle('overflow-hidden', show);
}

// Modal Editar Folga
function toggleEditTimeOffModal(show) {
    const modal = document.getElementById('editTimeOffModal');
    modal.classList.toggle('hidden', !show);
    document.body.classList.toggle('overflow-hidden', show);
}

// Modal Alterar Dados
function openProfileModal() {
    document.getElementById('profileModal').classList.remove('hidden');
    document.body.classList.add('overflow-hidden');
}

function closeProfileModal() {
    document.getElementById('profileModal').classList.add('hidden');
    document.body.classList.remove('overflow-hidden');
    document.getElementById('profileOption').value = '';
    document.getElementById('passwordFields').classList.add('hidden');
    document.getElementById('emailFields').classList.add('hidden');
}

function toggleProfileFields() {
    const option = document.getElementById('profileOption').value;
    document.getElementById('passwordFields').classList.add('hidden');
    document.getElementById('emailFields').classList.add('hidden');

    if (option === 'password')
        document.getElementById('passwordFields').classList.remove('hidden');
    else if (option === 'email')
        document.getElementById('emailFields').classList.remove('hidden');
}

// Fechar modais com ESC
document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') {
        toggleServiceModal(false);
        toggleEditModal(false);
        toggleTimeOffModal(false);
        toggleEditTimeOffModal(false);
        closeProfileModal();
    }
});

// Fechar modais clicando fora
document.getElementById('addServiceModal')?.addEventListener('click', function (e) {
    if (e.target === this) toggleServiceModal(false);
});

document.getElementById('editServiceModal')?.addEventListener('click', function (e) {
    if (e.target === this) toggleEditModal(false);
});

document.getElementById('addTimeOffModal')?.addEventListener('click', function (e) {
    if (e.target === this) toggleTimeOffModal(false);
});

document.getElementById('editTimeOffModal')?.addEventListener('click', function (e) {
    if (e.target === this) toggleEditTimeOffModal(false);
});

document.getElementById('profileModal')?.addEventListener('click', function (e) {
    if (e.target === this) closeProfileModal();
});

// Adicionar serviço à lista em memória
function addService() {
    const startDateTime = document.getElementById('startDateTime').value;
    const endDateTime = document.getElementById('endDateTime').value;
    const description = document.getElementById('description').value.trim();

    if (!startDateTime || !endDateTime) {
        alert('Informe data/hora de início e fim.');
        return;
    }

    const now = new Date();
    const start = new Date(startDateTime);
    const end = new Date(endDateTime);

    if (start >= end) {
        alert('A data/hora de início deve ser anterior à de fim.');
        return;
    }

    if (start > now) {
        alert('A data de início não pode ser no futuro.');
        return;
    }

    if (end > now) {
        alert('A data de fim não pode ser no futuro.');
        return;
    }

    services.push({ description, startDateTime, endDateTime });
    renderServices();

    document.getElementById('startDateTime').value = '';
    document.getElementById('endDateTime').value = '';
    document.getElementById('startDateTime').focus();
}

function renderServices() {
    const list = document.getElementById('serviceList');
    list.innerHTML = '';

    services.forEach((service, index) => {
        list.innerHTML += `
            <li class="flex justify-between items-center bg-gray-100 p-2 rounded">
                <div>
                    ${service.description ? `<div class="text-sm font-semibold">${service.description}</div>` : ''}
                    <span class="text-xs text-gray-600">
                        ${service.startDateTime} → ${service.endDateTime}
                    </span>
                </div>
                <button type="button"
                    onclick="removeService(${index})"
                    class="text-red-600 text-xs hover:underline">
                    Remover
                </button>
            </li>
        `;
    });
}

function removeService(index) {
    services.splice(index, 1);
    renderServices();
}

// Submit batch de serviços
function prepareSubmit(event) {
    if (services.length === 0) {
        alert('Adicione pelo menos um período.');
        event.preventDefault();
        return;
    }
    document.getElementById('servicesJson').value = JSON.stringify(services);
}

// Validação do formulário de editar serviço
document.getElementById('editServiceForm')?.addEventListener('submit', function (e) {
    const now = new Date();
    const start = new Date(document.getElementById('editStartDateTime').value);
    const end = new Date(document.getElementById('editEndDateTime').value);

    if (start > now) {
        e.preventDefault();
        alert('A data de início não pode ser no futuro.');
        return;
    }

    if (end > now) {
        e.preventDefault();
        alert('A data de fim não pode ser no futuro.');
        return;
    }

    if (end <= start) {
        e.preventDefault();
        alert('A data de fim deve ser após o início.');
    }
});

// Abrir modal de editar serviço
function openEditModal(button) {
    document.getElementById('editWorkId').value = button.dataset.id;
    document.getElementById('editDescription').value = button.dataset.description || '';
    document.getElementById('editStartDateTime').value = button.dataset.start;
    document.getElementById('editEndDateTime').value = button.dataset.end;
    toggleEditModal(true);
}

// Abrir modal de editar folga
function openEditTimeOffModal(button) {
    document.getElementById('editTimeOffId').value = button.getAttribute('data-id');
    document.getElementById('editSolicitationDate').value = button.getAttribute('data-solicitation');
    document.getElementById('editStartDate').value = button.getAttribute('data-start');
    document.getElementById('editEndDate').value = button.getAttribute('data-end') || '';
    document.getElementById('editPartialHours').value = button.getAttribute('data-partial') || '';
    toggleEditTimeOffModal(true);
}