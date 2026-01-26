// custom.js - Watch Shop JavaScript

document.addEventListener('DOMContentLoaded', function () {

    // ===== Smooth scroll to top button (nếu có nút back-to-top) =====
    const backToTopBtn = document.getElementById('back-to-top');
    if (backToTopBtn) {
        window.addEventListener('scroll', () => {
            if (window.scrollY > 400) {
                backToTopBtn.style.display = 'block';
            } else {
                backToTopBtn.style.display = 'none';
            }
        });

        backToTopBtn.addEventListener('click', (e) => {
            e.preventDefault();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    }

    // ===== Giỏ hàng - Cập nhật số lượng realtime (demo) =====
    const quantityInputs = document.querySelectorAll('.cart-quantity-input');
    quantityInputs.forEach(input => {
        input.addEventListener('change', function () {
            const quantity = parseInt(this.value);
            if (quantity < 1) this.value = 1;

            // Tính lại tổng tiền mỗi dòng (nếu có)
            const row = this.closest('tr');
            if (row) {
                const price = parseFloat(row.querySelector('.price-per-unit')?.dataset.price || 0);
                const totalCell = row.querySelector('.item-total');
                if (totalCell) {
                    const newTotal = (price * quantity).toLocaleString('vi-VN') + ' ₫';
                    totalCell.textContent = newTotal;
                }
            }

            // Tính tổng giỏ hàng (nếu muốn cập nhật realtime)
            updateCartTotal();
        });
    });

    function updateCartTotal() {
        let total = 0;
        document.querySelectorAll('.item-total').forEach(cell => {
            const value = parseFloat(cell.textContent.replace(/[^\d]/g, '')) || 0;
            total += value;
        });

        const totalElement = document.getElementById('cart-total');
        if (totalElement) {
            totalElement.textContent = total.toLocaleString('vi-VN') + ' ₫';
        }
    }

    // ===== Alert tự động ẩn sau 4 giây (ví dụ: đăng ký thành công, lỗi,...) =====
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.classList.add('fade');
            setTimeout(() => alert.remove(), 300);
        }, 4000);
    });

    // ===== Format giá tiền Việt Nam =====
    document.querySelectorAll('.price-format').forEach(el => {
        const price = parseFloat(el.textContent);
        if (!isNaN(price)) {
            el.textContent = price.toLocaleString('vi-VN') + ' ₫';
        }
    });

    // ===== Tooltip Bootstrap =====
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggerList.forEach(tooltipTriggerEl => {
        new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // ===== Carousel auto-play (nếu cần tùy chỉnh) =====
    const carousel = document.querySelector('.carousel');
    if (carousel) {
        new bootstrap.Carousel(carousel, {
            interval: 5000,
            ride: 'carousel'
        });
    }

    console.log("Custom JS loaded - Watch Shop ready! ⌚");
});