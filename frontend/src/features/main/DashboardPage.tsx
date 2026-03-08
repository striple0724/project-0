import { Link } from "react-router-dom";

const cards = [
  { title: "오늘 처리 작업", value: "42", caption: "전일 대비 +8" },
  { title: "대기 승인", value: "7", caption: "처리 필요" },
  { title: "비동기 작업", value: "3", caption: "실행 중" },
];

export function DashboardPage() {
  return (
    <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 p-6 md:p-8">
      <section className="rounded-2xl border border-slate-700 bg-[#0b1f3a]/70 p-6 shadow-[0_20px_60px_-30px_rgba(13,84,165,0.7)]">
        <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Dashboard</p>
        <h2 className="mt-2 text-3xl font-semibold text-slate-100">업무 운영 메인 화면</h2>
        <p className="mt-3 max-w-2xl text-sm text-slate-300">
          로그인 완료 후 진입하는 메인 페이지입니다. 좌측 메뉴의 Dashboard/Workbench를 통해 운영 화면으로 이동할 수 있습니다.
        </p>
        <div className="mt-5">
          <Link
            to="/workbench"
            className="inline-flex rounded-xl bg-sky-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-sky-400"
          >
            Workbench 이동
          </Link>
        </div>
      </section>

      <section className="grid grid-cols-1 gap-4 md:grid-cols-3">
        {cards.map((card) => (
          <article key={card.title} className="rounded-xl border border-slate-700 bg-slate-900/70 p-5">
            <p className="text-sm text-slate-400">{card.title}</p>
            <p className="mt-2 text-3xl font-semibold text-white">{card.value}</p>
            <p className="mt-2 text-xs text-slate-400">{card.caption}</p>
          </article>
        ))}
      </section>
    </div>
  );
}
