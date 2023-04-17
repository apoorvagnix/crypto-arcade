document.getElementById("propose-ad-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const whoAmI = document.getElementById("whoAmI").value;
    const whereTo = document.getElementById("whereTo").value;
    const adType = document.getElementById("adType").value;
    const adPlacement = document.getElementById("adPlacement").value;
    const adCost = document.getElementById("adCost").value;
    const adExpiry = document.getElementById("adExpiry").value;

    const response = await fetch("http://localhost:10050/api/propose", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({
            whoAmI,
            whereTo,
            adType,
            adPlacement,
            adCost: parseFloat(adCost) * 100,
            adExpiry,
        }),
    });

    if (response.ok) {
        alert("Advertisement proposal submitted successfully.");
        document.getElementById("propose-ad-form").reset();
    } else {
        alert("Error submitting advertisement proposal: " + response.statusText);
    }
});
