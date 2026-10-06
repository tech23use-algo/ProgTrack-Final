import { useState } from 'react'
import DashboardNav from './DashboardNav'

function StatCard({ label, value, detail, accent, progress }) {
  const barHeights = [38, 58, 78, 100]

  return (
    <article className="stat-card">
      <div className="stat-header">
        <span className="stat-label">{label}</span>
        {label !== 'Tasks done' && <span className={`stat-trend ${accent}`}>{detail}</span>}
      </div>

      <div className="stat-value">{value}</div>

      <div className="mini-chart" aria-hidden="true">
        {barHeights.map((barHeight, index) => (
          <span
            className={`bar bar-${index + 1} ${accent}`}
            style={{ height: `${progress ? Math.max(barHeight * progress / 100, 4) : 0}%` }}
            key={barHeight}
          />
        ))}
      </div>
    </article>
  )
}

function TaskItem({ task, onToggle }) {
  const progress = task.completed ? 100 : 0

  return (
    <div className={`task-item${task.completed ? ' completed' : ''}`}>
      <div className="task-copy">
        <h3>{task.title}</h3>
        <p>{task.details}</p>
        <span className="task-deadline">Due {task.deadline}</span>
      </div>

      <div className="task-progress-wrap">
        <div className="task-progress-bar" aria-label={`${progress}% complete`}>
          <span className="task-progress-fill" style={{ width: `${progress}%` }} />
        </div>
        <span className="task-progress-label">{progress}%</span>
        <button
          type="button"
          className="task-complete-toggle"
          aria-pressed={task.completed}
          onClick={() => onToggle(task.id)}
        >
          {task.completed ? 'Completed' : 'Mark done'}
        </button>
      </div>
    </div>
  )
}

function DashboardPage({ onNavigate, theme }) {
  const [tasks, setTasks] = useState([])

  const completedTasks = tasks.filter((task) => task.completed).length
  const progress = tasks.length === 0 ? 0 : Math.round((completedTasks / tasks.length) * 100)

  function toggleTask(taskId) {
    setTasks((currentTasks) => currentTasks.map((task) => (
      task.id === taskId ? { ...task, completed: !task.completed } : task
    )))
  }

  return (
    <main className={`dashboard-shell theme-${theme}`}>
      <DashboardNav activeSection="dashboard" onNavigate={onNavigate} />

      <section className="welcome-block">
        <div>
          <p className="eyebrow">Today&apos;s momentum</p>
          <h1>Welcome back, user</h1>
          <p className="welcome-copy">You&apos;re closer than yesterday. Keep the streak moving.</p>
        </div>
      </section>

      <section className="stats-grid">
        <StatCard label="Total tasks" value={tasks.length} detail={tasks.length === 0 ? 'No tasks yet' : 'Tasks in progress'} accent="green" progress={tasks.length ? 100 : 0} />
        <StatCard label="Tasks done" value={completedTasks} detail={`${progress}% complete`} accent="blue" progress={progress} />

        <div className="momentum-meter stat-card momentum-card" aria-label={`${progress} percent overall progress`}>
          <div className="momentum-meter-status">
            <span className="momentum-label">Overall progress</span>
            <strong>{progress}%</strong>
          </div>
          <div className="momentum-copy">
            <strong>{tasks.length === 0 ? 'No tasks yet' : `${tasks.length - completedTasks} tasks to go`}</strong>
            <div className="momentum-progress" aria-hidden="true">
              <span style={{ width: `${progress}%` }} />
            </div>
          </div>
        </div>
      </section>

      <section className="tasks-panel">
        <div className="section-header">
          <h2>My tasks</h2>
          <div className="section-actions">
            <button type="button" className="add-task-button">
              Learn skill
            </button>
            <button type="button">View all</button>
          </div>
        </div>

        <div className="task-list">
          {tasks.length === 0 ? (
            <p className="empty-task-state">No tasks added yet. Start by creating your first task.</p>
          ) : (
            tasks.map((task) => <TaskItem task={task} onToggle={toggleTask} key={task.id} />)
          )}
        </div>
      </section>
    </main>
  )
}

export default DashboardPage
