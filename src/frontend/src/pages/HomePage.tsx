import HealthStatus from '../HealthStatus'

export default function HomePage() {
  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Học Tin học 10, 11, 12</h1>
      <p className="mt-3 text-slate-600 dark:text-slate-400">
        Bài học Tin học THPT bằng tiếng Việt, kèm chatbot và AI Agent trả lời có trích dẫn nguồn.
      </p>

      <section
        aria-labelledby="health-title"
        className="mt-8 rounded-xl border border-slate-200 bg-white p-5 dark:border-slate-800 dark:bg-slate-900"
      >
        <h2
          id="health-title"
          className="text-sm font-semibold uppercase tracking-wide text-slate-600 dark:text-slate-400"
        >
          Trạng thái hệ thống
        </h2>
        <HealthStatus />
      </section>
    </>
  )
}
