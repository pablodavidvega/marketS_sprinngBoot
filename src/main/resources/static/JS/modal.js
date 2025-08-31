
    const modal = document.getElementById('modal');
    const openModal = document.getElementById('open-modal');
    const closeModal = document.querySelector('.close-modal');

    openModal.addEventListener('click', () => {
    modal.style.display = 'flex';
});

    closeModal.addEventListener('click', () => {
    modal.style.display = 'none';
});

    window.addEventListener('click', (event) => {
    if (event.target === modal) {
    modal.style.display = 'none';
}
});