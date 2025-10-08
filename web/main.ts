document.addEventListener('DOMContentLoaded', () => {
    const statsElement = document.getElementById('stats');
    if (statsElement) {
        statsElement.innerText = "Frame Stats - FPS: ~15, Resolution: 640x480";
    }
});