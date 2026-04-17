// Busca de fiscais
const searchInput = document.getElementById('searchFiscal');
const noResults = document.getElementById('noResults');

if (searchInput) {
    searchInput.addEventListener('input', function () {
        const searchTerm = this.value.toLowerCase().trim();
        let hasResults = false;

        const fiscalItems = document.querySelectorAll('.fiscal-item');

        fiscalItems.forEach(item => {
            const fiscalName = (item.getAttribute('data-name') || '').toLowerCase();

            if (fiscalName.includes(searchTerm)) {
                item.style.display = 'grid';
                hasResults = true;
            } else {
                item.style.display = 'none';
            }
        });

        if (noResults) {
            noResults.classList.toggle('hidden', hasResults || searchTerm === '');
        }
    });
}

// Modal Adicionar Fiscal
function toggleModal(show) {
    const modal = document.getElementById('addFiscalModal');
    modal.classList.toggle('hidden', !show);
    document.body.classList.toggle('overflow-hidden', show);
    if (show) document.getElementById('fullName').focus();
    else document.getElementById('addFiscalForm').reset();
}

// Modal Remover Fiscal
function toggleDeleteModal(show) {
    const modal = document.getElementById('deleteFiscalModal');
    modal.classList.toggle('hidden', !show);
    document.body.classList.toggle('overflow-hidden', show);
    if (!show) document.getElementById('registrationDelete').value = '';
}

// Modal Adicionar Feriado
function toggleHolidayModal(show) {
    const modal = document.getElementById('addHolidayModal');
    modal.classList.toggle('hidden', !show);
    document.body.classList.toggle('overflow-hidden', show);
    if (show) document.getElementById('holidayDate').focus();
    else document.getElementById('addHolidayForm').reset();
}

// Modal Editar Feriado
function toggleEditHolidayModal(show) {
    const modal = document.getElementById('editHolidayModal');
    modal.classList.toggle('hidden', !show);
    document.body.classList.toggle('overflow-hidden', show);
    if (!show) document.getElementById('editHolidayForm').reset();
}

// Fechar modais com ESC
document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') {
        toggleModal(false);
        toggleDeleteModal(false);
        toggleHolidayModal(false);
        toggleEditHolidayModal(false);
    }
});

// Fechar modais clicando fora
document.getElementById('addFiscalModal').addEventListener('click', function (e) {
    if (e.target === this) toggleModal(false);
});

document.getElementById('deleteFiscalModal').addEventListener('click', function (e) {
    if (e.target === this) toggleDeleteModal(false);
});

document.getElementById('addHolidayModal').addEventListener('click', function (e) {
    if (e.target === this) toggleHolidayModal(false);
});

document.getElementById('editHolidayModal').addEventListener('click', function (e) {
    if (e.target === this) toggleEditHolidayModal(false);
});

// Abrir modal de editar feriado
document.addEventListener('click', function (event) {
    const editButton = event.target.closest('button[data-id]');
    if (editButton && editButton.hasAttribute('data-date')) {
        document.getElementById('editHolidayId').value = editButton.getAttribute('data-id');
        document.getElementById('editHolidayDate').value = editButton.getAttribute('data-date');
        document.getElementById('editHolidayDescription').value = editButton.getAttribute('data-description');
        toggleEditHolidayModal(true);
    }
});

// Validação formulário fiscal
document.getElementById('addFiscalForm').addEventListener('submit', function (event) {
    const fullName = document.getElementById('fullName').value.trim();
    const registration = document.getElementById('registration').value.trim();
    const userType = document.getElementById('userType').value;

    if (!fullName || !registration || !userType) {
        event.preventDefault();
        alert('Por favor, preencha todos os campos obrigatórios.');
        return;
    }

    if (fullName.length < 2) {
        event.preventDefault();
        alert('Nome completo deve ter pelo menos 2 caracteres.');
    }
});

// Validação formulário feriado
document.getElementById('addHolidayForm').addEventListener('submit', function (event) {
    const date = document.getElementById('holidayDate').value;
    const description = document.getElementById('holidayDescription').value.trim();

    if (!date || !description) {
        event.preventDefault();
        alert('Por favor, preencha todos os campos obrigatórios.');
        return;
    }

    if (description.length < 3) {
        event.preventDefault();
        alert('Descrição deve ter pelo menos 3 caracteres.');
    }
});

// Modal Editar Fiscal
function openEditFiscalModal(button) {
    const id = button.getAttribute('data-id');
    const name = button.getAttribute('data-name');
    const email = button.getAttribute('data-email');
    const userType = button.getAttribute('data-usertype');

    document.getElementById('editFiscalFullName').value = name;
    document.getElementById('editFiscalEmail').value = email;
    document.getElementById('editFiscalUserType').value = userType;

    document.getElementById('editFiscalForm').action =
        `/controle_de_folgas/dashboard/administrador/fiscal/${id}/editar`;

    toggleEditFiscalModal(true);
}

function toggleEditFiscalModal(show) {
    const modal = document.getElementById('editFiscalModal');
    modal.classList.toggle('hidden', !show);
    document.body.classList.toggle('overflow-hidden', show);

    if (show) {
        document.getElementById('editFiscalFullName').focus();
    } else {
        document.getElementById('editFiscalForm').reset();
    }
}

// Fechar modal clicando fora
document.getElementById('editFiscalModal').addEventListener('click', function (e) {
    if (e.target === this) toggleEditFiscalModal(false);
});

// Validação formulário editar fiscal
document.getElementById('editFiscalForm').addEventListener('submit', function (event) {
    const fullName = document.getElementById('editFiscalFullName').value.trim();
    const email = document.getElementById('editFiscalEmail').value.trim();
    const userType = document.getElementById('editFiscalUserType').value;

    if (!fullName || !email || !userType) {
            event.preventDefault();
            alert('Por favor, preencha todos os campos obrigatórios.');
            return;
   }
});