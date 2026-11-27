import { Link, useNavigate, useSearchParams } from 'react-router'
import { fetchCatalog, fetchCategories, type CatalogQuery, type Category } from '../catalogApi'
import { SECONDARY } from '../buttons'
import CourseCard from '../CourseCard'
import { LEVEL_LABELS } from '../courseLabels'
import LoadError from '../LoadError'
import { useLoad } from '../useLoad'
import { useMyProgress } from '../useMyProgress'

const PATH = '/danh-sach-khoa-hoc'

type Option = [value: string, label: string]

// Tên tham số trên địa chỉ trang (tiếng Việt) và trường tương ứng của API.
const FIELDS = [
  { param: 'nhom', key: 'category' },
  { param: 'trinh-do', key: 'level' },
  { param: 'thoi-luong', key: 'duration' },
  { param: 'hoc-phi', key: 'price' },
  { param: 'sap-xep', key: 'sort' },
] as const

// Mốc lọc do giao diện đặt, API nhận khoảng bất kỳ. Khóa chưa nhập số giờ hoặc học phí (0) không thuộc mốc nào.
const LEVELS: Option[] = Object.entries(LEVEL_LABELS)
const DURATIONS: Option[] = [
  ['1-30', 'Đến 30 giờ'],
  ['31-60', '31 đến 60 giờ'],
  ['61-', 'Trên 60 giờ'],
]
const PRICES: Option[] = [
  ['1-999999', 'Dưới 1 triệu đồng'],
  ['1000000-3000000', '1 đến 3 triệu đồng'],
  ['3000001-', 'Trên 3 triệu đồng'],
]
const SORTS: Option[] = [
  ['price_asc', 'Học phí thấp đến cao'],
  ['price_desc', 'Học phí cao đến thấp'],
]

// Tham số lạ trong địa chỉ coi như không có, không gửi lên máy chủ. Slug nhóm chỉ kiểm dạng: nhóm lạ ra danh sách rỗng.
function parseQuery(params: URLSearchParams): CatalogQuery {
  const query: Record<string, string> = {}
  const allowed: Record<string, Option[]> = { level: LEVELS, duration: DURATIONS, price: PRICES, sort: SORTS }
  for (const { param, key } of FIELDS) {
    const value = params.get(param)
    if (value === null) continue
    const valid =
      key === 'category' ? /^[a-z0-9]+(-[a-z0-9]+)*$/.test(value) : allowed[key].some(([option]) => option === value)
    if (valid) query[key] = value
  }
  return query as CatalogQuery
}

function hrefFor(query: CatalogQuery): string {
  const params = new URLSearchParams()
  for (const { param, key } of FIELDS) {
    const value = query[key]
    if (value) params.set(param, value)
  }
  const search = params.toString()
  return search === '' ? PATH : `${PATH}?${search}`
}

type FilterSelectProps = {
  id: string
  label: string
  empty: string
  value: string
  options: Option[]
  onChange: (value: string) => void
}

function FilterSelect({ id, label, empty, value, options, onChange }: FilterSelectProps) {
  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium">
        {label}
      </label>
      <select id={id} value={value} onChange={(event) => onChange(event.target.value)} className="field mt-1 w-full">
        <option value="">{empty}</option>
        {options.map(([optionValue, optionLabel]) => (
          <option key={optionValue} value={optionValue}>
            {optionLabel}
          </option>
        ))}
      </select>
    </div>
  )
}

function Filters({ query, categories }: { query: CatalogQuery; categories: Category[] }) {
  const navigate = useNavigate()
  const change = (key: keyof CatalogQuery) => (value: string) =>
    navigate(hrefFor({ ...query, [key]: value === '' ? undefined : value }))

  return (
    <form aria-label="Bộ lọc khóa học" onSubmit={(event) => event.preventDefault()}>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <FilterSelect
          id="filter-category"
          label="Chương trình"
          empty="Tất cả chương trình"
          value={query.category ?? ''}
          options={categories.map((category) => [category.slug, category.name])}
          onChange={change('category')}
        />
        <FilterSelect
          id="filter-level"
          label="Trình độ"
          empty="Tất cả trình độ"
          value={query.level ?? ''}
          options={LEVELS}
          onChange={change('level')}
        />
        <FilterSelect
          id="filter-duration"
          label="Thời lượng"
          empty="Mọi thời lượng"
          value={query.duration ?? ''}
          options={DURATIONS}
          onChange={change('duration')}
        />
        <FilterSelect
          id="filter-price"
          label="Học phí"
          empty="Mọi mức học phí"
          value={query.price ?? ''}
          options={PRICES}
          onChange={change('price')}
        />
        <FilterSelect
          id="filter-sort"
          label="Sắp xếp"
          empty="Mặc định"
          value={query.sort ?? ''}
          options={SORTS}
          onChange={change('sort')}
        />
      </div>
    </form>
  )
}

function Results({ query }: { query: CatalogQuery }) {
  const { state, reload } = useLoad(() => fetchCatalog(query), hrefFor(query))
  const progress = useMyProgress()
  // Tiến độ chỉ là phần phụ: chưa đăng nhập, đang tải hoặc lỗi thì thẻ khóa hiện như thường.
  const mine = progress.state.kind === 'ok' ? (progress.state.data ?? []) : []

  if (state.kind === 'loading') return <p className="mt-6 text-muted">Đang tải danh sách khóa học...</p>
  if (state.kind === 'error') return <LoadError message="Không tải được danh sách khóa học." onRetry={reload} />
  if (state.data.length === 0) return <p className="mt-6 text-lg">Không có khóa học phù hợp.</p>
  return (
    <>
      <p role="status" className="mt-6 text-muted">{`${state.data.length} khóa học`}</p>
      <ul className="mt-4 grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
        {state.data.map((course) => (
          <CourseCard
            key={course.id}
            course={course}
            heading="h2"
            mine={mine.find((p) => p.courseSlug === course.slug)}
          />
        ))}
      </ul>
    </>
  )
}

export default function CourseListPage() {
  const [params] = useSearchParams()
  const query = parseQuery(params)
  // Danh sách nhóm chỉ để đổ ô chọn; tải lỗi thì vẫn xem và lọc theo các tiêu chí khác được.
  const categories = useLoad(fetchCategories, 'categories')
  const categoryList = categories.state.kind === 'ok' ? categories.state.data : []
  const filtered = Object.keys(query).length > 0

  return (
    <>
      <h1 className="text-4xl font-bold leading-tight tracking-tight">Danh sách khóa học</h1>
      <p className="mt-2 max-w-2xl text-muted">Chọn chương trình, trình độ, thời lượng và mức học phí phù hợp.</p>
      <div className="mt-6">
        <Filters query={query} categories={categoryList} />
        {filtered && (
          <p className="mt-4">
            <Link to={PATH} className={SECONDARY}>
              Xóa bộ lọc
            </Link>
          </p>
        )}
      </div>
      <Results query={query} />
    </>
  )
}
