function formatRegistration(input) {
    let value = input.value.replace(/\D/g, '');
    if (value.length > 6) value = value.substring(0, 6);
    if (value.length > 5) value = value.substring(0, 5) + '-' + value.substring(5);
    input.value = value;
}

// Auto-dismiss das mensagens de feedback
document.addEventListener('DOMContentLoaded', function () {
    const messages = document.querySelectorAll('[data-feedback]');

    messages.forEach(function (message) {
        // Começa a sumir após 4 segundos
        setTimeout(function () {
            message.classList.add('transition-opacity', 'duration-700', 'opacity-0');

            // Remove do DOM após a animação terminar
            setTimeout(function () {
                message.remove();
            }, 700);
        }, 2000);
    });
});