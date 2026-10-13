import { useEffect, useState } from 'react';
import { Link, Routes, Route, Navigate, useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
import './App.css';

const api = axios.create({ baseURL: '/api' });
api.interceptors.request.use((c) => {
  const t = localStorage.getItem('goal_token');
  if (t) c.headers.Authorization = `Bearer ${t}`;
  return c;
});
api.interceptors.response.use(
  (r) => r,
  (e) => {
    if (e.response?.status === 401) {
      localStorage.clear();
      window.location = '/login';
    }
    return Promise.reject(e);
  }
);
const error = (e) =>
  e.response?.data?.message ||
  (e.code === 'ERR_NETWORK'
    ? 'Cannot reach the backend. Check that Spring Boot is running on port 8080.'
    : 'Request failed. Please try again.');
const user = () => JSON.parse(localStorage.getItem('goal_user') || 'null');

function Layout({ kind, children }) {
  const nav = useNavigate(),
    u = user();
  const links =
    kind === 'user'
      ? [
          ['/app', 'Dashboard'],
          ['/app/goals', 'My Goals'],
          ['/app/new', 'New Goal'],
          ['/app/learn', 'Learn a Skill'],
        ]
      : kind === 'manager'
      ? [
          ['/manager', 'Team Overview'],
          ['/manager/team', 'Team Members'],
          ['/manager/analytics', 'Analytics'],
        ]
      : [
          ['/admin', 'System Overview'],
          ['/admin/users', 'User Management'],
          ['/admin/audit', 'Audit Log'],
        ];
  return (
    <div className={`role-app ${kind}`}>
      <aside>
        <Link className="brand" to={kind === 'user' ? '/app' : `/${kind}`}>
          Prog<span>Track</span>
        </Link>
        <div className="role-label">{kind.toUpperCase()} SPACE</div>
        <nav>
          {links.map(([path, label]) => (
            <Link key={path} to={path}>
              {label}
            </Link>
          ))}
        </nav>
        <button
          className="signout"
          onClick={() => {
            localStorage.clear();
            nav('/login');
          }}
        >
          Sign out
        </button>
      </aside>
      <div className="role-main">
        <header>
          <span className="mobile-title">{links[0][1]}</span>
          <span className="profile">
            {u?.displayName} <b>{u?.role}</b>
          </span>
        </header>
        <main>{children}</main>
      </div>
    </div>
  );
}

function Guard({ role, children }) {
  const u = user();
  if (!u) return <Navigate to="/login" replace />;
  if (role && u.role !== role)
    return <Navigate to={u.role === 'USER' ? '/app' : `/${u.role.toLowerCase()}`} replace />;
  return <Layout kind={role.toLowerCase()}>{children}</Layout>;
}

function Auth({ register = false }) {
  const nav = useNavigate(),
    [form, setForm] = useState({ email: '', password: '', displayName: '' }),
    [message, setMessage] = useState('');
  const submit = async (e) => {
    e.preventDefault();
    try {
      const r = await api.post(`/auth/${register ? 'register' : 'login'}`, form);
      localStorage.setItem('goal_token', r.data.token);
      localStorage.setItem('goal_user', JSON.stringify(r.data));
      nav(r.data.role === 'USER' ? '/app' : `/${r.data.role.toLowerCase()}`);
    } catch (e) {
      setMessage(error(e));
    }
  };
  return (
    <div className="auth-page">
      <div className="auth-art">
        <span>ProgTrack</span>
        <h1>
          Different roles.<br />
          <i>One direction.</i>
        </h1>
        <p>
          Make meaningful progress visible, whether you are building it, guiding it, or keeping the whole
          system moving.
        </p>
      </div>
      <form className="auth-card" onSubmit={submit}>
        <div className="eyebrow">{register ? 'CREATE ACCOUNT' : 'WELCOME BACK'}</div>
        <h2>{register ? 'Start tracking.' : 'Good to see you.'}</h2>
        {register && (
          <input
            placeholder="Name"
            required
            value={form.displayName}
            onChange={(e) => setForm({ ...form, displayName: e.target.value })}
          />
        )}
        <input
          type="email"
          placeholder="Email"
          required
          value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })}
        />
        <input
          type="password"
          placeholder="Password"
          minLength="8"
          required
          value={form.password}
          onChange={(e) => setForm({ ...form, password: e.target.value })}
        />
        {message && <div className="error">{message}</div>}
        <button className="primary">
          {register ? 'Create account' : 'Sign in'} <span>→</span>
        </button>
        <p className="switch">
          {register ? 'Already have an account?' : 'New here?'}{' '}
          <Link to={register ? '/login' : '/register'}>
            {register ? 'Sign in' : 'Create an account'}
          </Link>
        </p>
      </form>
    </div>
  );
}

function UserDashboard() {
  const [d, setD] = useState(null);
  useEffect(() => {
    api.get('/user/dashboard').then((r) => setD(r.data));
  }, []);
  if (!d) return <Loading />;
  return (
    <>
      <Intro title="Make progress visible." action="+ New goal" to="/app/new" />
      <section className="stats">
        <div>
          <span>Total goals</span>
          <strong>{d.totalGoals}</strong>
        </div>
        <div>
          <span>Overall completion</span>
          <strong>{Math.round(d.completion)}%</strong>
        </div>
        <div>
          <span>Next deadline</span>
          <strong>{d.upcoming[0]?.targetDate || 'None'}</strong>
        </div>
      </section>
      <h2>Active goals</h2>
      <div className="goal-grid">
        {d.goals.map((g) => (
          <GoalCard key={g.id} g={g} />
        ))}
        {!d.goals.length && <Empty to="/app/new">Create your first goal →</Empty>}
      </div>
    </>
  );
}

function UserGoals() {
  const [goals, setGoals] = useState([]);
  useEffect(() => {
    api.get('/user/goals').then((r) => setGoals(r.data));
  }, []);
  return (
    <>
      <Intro title="My goals." action="+ New goal" to="/app/new" />
      <div className="goal-grid">
        {goals.map((g) => (
          <GoalCard key={g.id} g={g} />
        ))}
      </div>
    </>
  );
}

function GoalCard({ g }) {
  return (
    <Link className="goal-card" to={`/app/goals/${g.id}`}>
      <div className="goal-top">
        <span className="goal-index">0{g.id}</span>
        <span className="deadline">{g.targetDate ? `Due ${g.targetDate}` : 'Open-ended'}</span>
      </div>
      <h3>{g.title}</h3>
      {g.assignedByManagerId && <small className="assigned">Assigned by manager</small>}
      <p>{g.description || 'No description yet.'}</p>
      <div className="bar">
        <span style={{ width: `${g.completion}%` }} />
      </div>
      <div className="card-footer">
        <span>{Math.round(g.completion)}% complete</span>
        <span>{g.milestones.length} steps</span>
      </div>
    </Link>
  );
}

function Intro({ title, action, to }) {
  return (
    <div className="page-intro">
      <div>
        <div className="eyebrow">YOUR WORKSPACE</div>
        <h1>{title}</h1>
        <p className="muted">One clear next step is enough for today.</p>
      </div>
      <Link className="primary compact" to={to}>
        {action}
      </Link>
    </div>
  );
}

function NewGoal({ learn = false }) {
  const nav = useNavigate(),
    [form, setForm] = useState(
      learn
        ? { skill: '', days: 14, hoursPerDay: 1 }
        : { title: '', description: '', targetDate: '', hoursPerDay: 1 }
    ),
    [message, setMessage] = useState(''),
    [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setMessage('');
    try {
      const payload = learn
        ? { skill: form.skill.trim(), days: Number(form.days), hoursPerDay: Number(form.hoursPerDay) }
        : {
            title: form.title.trim(),
            description: form.description,
            targetDate: form.targetDate || null,
            hoursPerDay: form.hoursPerDay ? Number(form.hoursPerDay) : 1,
          };
      const r = await api.post(learn ? '/user/skill-plans' : '/user/goals', payload);
      nav(`/app/goals/${r.data.id}`);
    } catch (e) {
      setMessage(error(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="form-wrap">
      <Link className="back" to="/app">
        ← Dashboard
      </Link>
      <div className="eyebrow">{learn ? 'LEARN A SKILL' : 'NEW GOAL'}</div>
      <h1>{learn ? 'Learn something new.' : 'Name the outcome.'}</h1>
      <p className="muted">
        {learn
          ? 'Turn any skill into a progressive day-by-day practice curriculum adjusted to your daily time.'
          : 'Give your attention a direction, then let the details follow.'}
      </p>
      <form className="goal-form" onSubmit={submit}>
        {learn ? (
          <>
            <label>
              What do you want to learn?
              <input
                required
                minLength="2"
                placeholder="e.g. Piano, Guitar, Python..."
                value={form.skill}
                onChange={(e) => setForm({ ...form, skill: e.target.value })}
              />
            </label>
            <div className="two">
              <label>
                Days
                <input
                  type="number"
                  min="1"
                  max="365"
                  value={form.days}
                  onChange={(e) => setForm({ ...form, days: Number(e.target.value) })}
                />
              </label>
              <label>
                Hours per day
                <input
                  type="number"
                  min=".5"
                  max="12"
                  step=".5"
                  value={form.hoursPerDay}
                  onChange={(e) => setForm({ ...form, hoursPerDay: Number(e.target.value) })}
                />
              </label>
            </div>
          </>
        ) : (
          <>
            <label>
              Goal title
              <input
                required
                minLength="2"
                maxLength="120"
                placeholder="e.g. Launch my portfolio"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
              />
            </label>
            <label>
              Description
              <textarea
                placeholder="Describe your goal and what you want to achieve..."
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </label>
            <div className="two">
              <label>
                Target date
                <input
                  type="date"
                  value={form.targetDate}
                  onChange={(e) => setForm({ ...form, targetDate: e.target.value })}
                />
              </label>
              <label>
                Hours per day
                <input
                  type="number"
                  min=".5"
                  max="12"
                  step=".5"
                  value={form.hoursPerDay}
                  onChange={(e) => setForm({ ...form, hoursPerDay: Number(e.target.value) })}
                />
              </label>
            </div>
          </>
        )}
        {message && <div className="error">{message}</div>}
        <button className="primary" disabled={busy}>
          {busy ? '✦ Building curriculum with AI...' : learn ? 'Build my plan' : 'Create goal'}{' '}
          <span>→</span>
        </button>
      </form>
    </div>
  );
}

function GoalDetail() {
  const { id } = useParams(),
    nav = useNavigate(),
    [g, setG] = useState(null),
    [message, setMessage] = useState(''),
    [busy, setBusy] = useState(false);

  const load = () => api.get(`/user/goals/${id}`).then((r) => setG(r.data));
  useEffect(() => {
    load();
  }, [id]);

  const toggle = async (m) => {
    await api.patch(`/user/milestones/${m.id}`, { toggle: true });
    load();
  };

  const ai = async () => {
    setBusy(true);
    setMessage('');
    try {
      await api.post(`/user/goals/${id}/ai-breakdown`);
      load();
    } catch (e) {
      setMessage(error(e));
    } finally {
      setBusy(false);
    }
  };

  if (!g) return <Loading />;

  return (
    <div className="detail">
      <Link className="back" to="/app">
        ← Dashboard
      </Link>
      <div className="detail-head">
        <div>
          <div className="eyebrow">
            GOAL 0{g.id} {g.hoursPerDay ? `· ${g.hoursPerDay} HR/DAY` : ''}
          </div>
          <h1>{g.title}</h1>
          <p className="muted">{g.description}</p>
        </div>
        <button
          className="danger"
          onClick={async () => {
            await api.delete(`/user/goals/${id}`);
            nav('/app');
          }}
        >
          Delete
        </button>
      </div>
      <div className="detail-progress">
        <strong>{Math.round(g.completion)}%</strong>
        <span>
          {g.milestones.filter((m) => m.status === 'DONE').length} of {g.milestones.length} complete
        </span>
        <div className="bar">
          <span style={{ width: `${g.completion}%` }} />
        </div>
      </div>
      <div className="detail-actions">
        <button className="primary" onClick={ai} disabled={busy}>
          {busy
            ? 'Generating curriculum with AI...'
            : g.milestones.length
            ? '✦ Regenerate steps with AI'
            : '✦ Generate steps with AI'}
        </button>
      </div>
      {message && <div className="error">{message}</div>}
      <div className="milestones">
        {g.milestones.map((m) => (
          <div className={`milestone ${m.status === 'DONE' ? 'done' : ''}`} key={m.id}>
            <button className="check" onClick={() => toggle(m)}>
              {m.status === 'DONE' ? '✓' : ''}
            </button>
            <div>
              <strong>{m.title}</strong>
              <p>{m.description}</p>
            </div>
            <span>{m.estimatedMinutes ? `${m.estimatedMinutes} min` : ''}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function ManagerDashboard() {
  const [team, setTeam] = useState([]),
    [a, setA] = useState(null);
  useEffect(() => {
    Promise.all([api.get('/manager/team'), api.get('/manager/analytics')]).then(([t, x]) => {
      setTeam(t.data);
      setA(x.data);
    });
  }, []);
  if (!a) return <Loading />;
  return (
    <>
      <Intro title="Guide the work." action="Team members" to="/manager/team" />
      <section className="stats">
        <div>
          <span>Team members</span>
          <strong>{team.length}</strong>
        </div>
        <div>
          <span>Average completion</span>
          <strong>{Math.round(a.averageCompletion)}%</strong>
        </div>
        <div>
          <span>Overdue goals</span>
          <strong>{a.overdueGoals}</strong>
        </div>
      </section>
      <h2>Team pulse</h2>
      <div className="team-grid">
        {team.map((m) => (
          <div className="member-card" key={m.id}>
            <div className="member-avatar">{m.displayName[0]}</div>
            <h3>{m.displayName}</h3>
            <p>{m.email}</p>
            <div className="bar">
              <span style={{ width: `${m.completion}%` }} />
            </div>
            <small>
              {Math.round(m.completion)}% · {m.goalCount} goals
            </small>
            <Link to={`/manager/team/${m.id}`}>View goals →</Link>
          </div>
        ))}
      </div>
    </>
  );
}

function ManagerTeam() {
  const [team, setTeam] = useState([]);
  useEffect(() => {
    api.get('/manager/team').then((r) => setTeam(r.data));
  }, []);
  return (
    <>
      <Intro title="Your team." action="Refresh" to="/manager/team" />
      <div className="table">
        {team.map((m) => (
          <Link to={`/manager/team/${m.id}`} key={m.id}>
            <b>{m.displayName}</b>
            <span>{m.email}</span>
            <span>{Math.round(m.completion)}%</span>
            <span>{m.goalCount} goals</span>
          </Link>
        ))}
      </div>
    </>
  );
}

function MemberGoals() {
  const { id } = useParams(),
    [goals, setGoals] = useState([]);
  useEffect(() => {
    api.get(`/manager/team/${id}/goals`).then((r) => setGoals(r.data));
  }, [id]);
  return (
    <>
      <Link className="back" to="/manager/team">
        ← Team
      </Link>
      <h1>Member goals.</h1>
      <div className="goal-grid">
        {goals.map((g) => (
          <GoalCard key={g.id} g={g} />
        ))}
      </div>
    </>
  );
}

function AdminDashboard() {
  const [s, setS] = useState(null);
  useEffect(() => {
    api.get('/admin/stats').then((r) => setS(r.data));
  }, []);
  if (!s) return <Loading />;
  return (
    <>
      <Intro title="System overview." action="Manage users" to="/admin/users" />
      <section className="stats">
        <div>
          <span>Total users</span>
          <strong>{s.users}</strong>
        </div>
        <div>
          <span>Managers</span>
          <strong>{s.managers}</strong>
        </div>
        <div>
          <span>Goals</span>
          <strong>{s.goals}</strong>
        </div>
        <div>
          <span>Completion</span>
          <strong>{Math.round(s.overallCompletion)}%</strong>
        </div>
      </section>
      <h2>Administration</h2>
      <div className="admin-links">
        <Link to="/admin/users">User management →</Link>
        <Link to="/admin/audit">Audit log →</Link>
      </div>
    </>
  );
}

function AdminUsers() {
  const [users, setUsers] = useState([]);
  useEffect(() => {
    api.get('/admin/users').then((r) => setUsers(r.data.content || []));
  }, []);
  return (
    <>
      <Intro title="Manage access." action="Refresh" to="/admin/users" />
      <div className="table">
        {users.map((u) => (
          <div key={u.id}>
            <b>{u.displayName}</b>
            <span>{u.email}</span>
            <span>{u.role}</span>
            <span>{u.active ? 'Active' : 'Inactive'}</span>
          </div>
        ))}
      </div>
    </>
  );
}

function AdminAudit() {
  const [logs, setLogs] = useState([]);
  useEffect(() => {
    api.get('/admin/audit-log').then((r) => setLogs(r.data));
  }, []);
  return (
    <>
      <Intro title="Audit history." action="System overview" to="/admin" />
      <div className="table">
        {logs.map((x) => (
          <div key={x.id}>
            <b>{x.action}</b>
            <span>Actor {x.actorId}</span>
            <span>Target {x.targetId || '—'}</span>
            <span>{new Date(x.timestamp).toLocaleString()}</span>
          </div>
        ))}
      </div>
    </>
  );
}

function Loading() {
  return <div className="loading">Loading...</div>;
}

function Empty({ children, to }) {
  return (
    <div className="empty">
      <Link to={to}>{children}</Link>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Auth />} />
      <Route path="/register" element={<Auth register />} />
      <Route
        path="/app"
        element={
          <Guard role="USER">
            <UserDashboard />
          </Guard>
        }
      />
      <Route
        path="/app/goals"
        element={
          <Guard role="USER">
            <UserGoals />
          </Guard>
        }
      />
      <Route
        path="/app/new"
        element={
          <Guard role="USER">
            <NewGoal />
          </Guard>
        }
      />
      <Route
        path="/app/learn"
        element={
          <Guard role="USER">
            <NewGoal learn />
          </Guard>
        }
      />
      <Route
        path="/app/goals/:id"
        element={
          <Guard role="USER">
            <GoalDetail />
          </Guard>
        }
      />
      <Route
        path="/manager"
        element={
          <Guard role="MANAGER">
            <ManagerDashboard />
          </Guard>
        }
      />
      <Route
        path="/manager/team"
        element={
          <Guard role="MANAGER">
            <ManagerTeam />
          </Guard>
        }
      />
      <Route
        path="/manager/team/:id"
        element={
          <Guard role="MANAGER">
            <MemberGoals />
          </Guard>
        }
      />
      <Route
        path="/manager/analytics"
        element={
          <Guard role="MANAGER">
            <ManagerDashboard />
          </Guard>
        }
      />
      <Route
        path="/admin"
        element={
          <Guard role="ADMIN">
            <AdminDashboard />
          </Guard>
        }
      />
      <Route
        path="/admin/users"
        element={
          <Guard role="ADMIN">
            <AdminUsers />
          </Guard>
        }
      />
      <Route
        path="/admin/audit"
        element={
          <Guard role="ADMIN">
            <AdminAudit />
          </Guard>
        }
      />
      <Route path="*" element={<Navigate to="/login" />} />
    </Routes>
  );
}
