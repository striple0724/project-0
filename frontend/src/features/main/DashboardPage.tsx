const cards = [
  { title: "오늘 처리 작업", value: "42", caption: "전일 대비 +8" },
  { title: "대기 승인", value: "7", caption: "처리 필요" },
  { title: "비동기 작업", value: "3", caption: "실행 중" },
];

export function DashboardPage() {
  return (
    <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 p-6 md:p-8">
      <section className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {cards.map((card) => (
          <article key={card.title} className="rounded-xl border border-[var(--border-main)] bg-[var(--bg-card)] p-5 shadow-sm">
            <p className="text-sm text-[var(--text-secondary)]">{card.title}</p>
            <p className="mt-2 text-3xl font-semibold text-[var(--text-primary)]">{card.value}</p>
            <p className="mt-2 text-xs text-[var(--text-secondary)]">{card.caption}</p>
          </article>
        ))}
      </section>
    </div>
  );
}
