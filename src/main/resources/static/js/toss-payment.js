document.addEventListener("DOMContentLoaded", function () {
    const button = document.getElementById("toss-payment-button");
    if (!button || button.disabled) return;
    const errorBox = document.getElementById("payment-error");
    button.addEventListener("click", async function () {
        button.disabled = true;
        errorBox.hidden = true;
        try {
            const tossPayments = TossPayments(button.dataset.clientKey);
            const payment = tossPayments.payment({customerKey: TossPayments.ANONYMOUS});
            await payment.requestPayment({
                method: "CARD",
                amount: {currency: "KRW", value: Number(button.dataset.amount)},
                orderId: button.dataset.orderId,
                orderName: button.dataset.orderName,
                customerName: button.dataset.customerName,
                customerEmail: button.dataset.customerEmail,
                successUrl: button.dataset.successUrl,
                failUrl: button.dataset.failUrl
            });
        } catch (error) {
            errorBox.textContent = error && error.code === "USER_CANCEL" ? "결제가 취소되었습니다. 다시 시도할 수 있습니다." : "결제창을 열지 못했습니다. 잠시 후 다시 시도해 주세요.";
            errorBox.hidden = false;
            button.disabled = false;
        }
    });
});
