async function fetchProposals() {
    const response = await fetch("http://localhost:10050/api/fetch-proposals");
    const proposals = await response.json();

    const container = document.getElementById("proposals-container");
    container.innerHTML = "";

    proposals.forEach((proposal) => {
        const proposalElement = document.createElement("div");
        proposalElement.innerHTML = `
            <h3>Ad Proposal: ${proposal.linearId}</h3>
            <p>Advertiser: ${proposal.whoAmI}</p>
            <p>Ad Type: ${proposal.adType}</p>
            <p>Ad Placement: ${proposal.adPlacement}</p>
            <p>Ad Cost: $${(proposal.adCost / 100).toFixed(2)}</p>
            <p>Ad Expiry: ${proposal.adExpiry}</p>
            <button onclick="acceptProposal('${proposal.linearId}')">Accept</button>
            <button onclick="rejectProposal('${proposal.linearId}')">Reject</button>
        `;

        container.appendChild(proposalElement);
    });
}

async function acceptProposal(linearId) {
    const response = await fetch(`http://localhost:10050/api/accept-proposal/${linearId}`, { method: "POST" });

    if (response.ok) {
        alert("Advertisement proposal accepted.");
        fetchProposals();
    } else {
        alert("Error accepting advertisement proposal: " + response.statusText);
    }
}

async function rejectProposal(linearId) {
    const response = await fetch(`http://localhost:10050/api/reject-proposal/${linearId}`, { method: "POST" });

    if (response.ok) {
        alert("Advertisement proposal rejected.");
        fetchProposals();
    } else {
        alert("Error rejecting advertisement proposal: " + response.statusText);
    }
}

document.getElementById("fetch-proposals").addEventListener("click", fetchProposals);
