document.addEventListener("DOMContentLoaded", () => {
    const nav = document.querySelector(".admin-menu-shell nav");
    const active = nav?.querySelector("a.active");
    const cue = document.querySelector(".admin-scroll-cue");
    if (!nav) return;

    active?.scrollIntoView({block: "nearest", inline: "center"});
    const updateCue = () => cue?.classList.toggle("is-hidden", nav.scrollLeft + nav.clientWidth >= nav.scrollWidth - 4);
    nav.addEventListener("scroll", updateCue, {passive: true});
    window.addEventListener("resize", updateCue, {passive: true});
    updateCue();
});
