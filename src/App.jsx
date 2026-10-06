import './App.css'
import { useState } from 'react'
import DashboardPage from './components/DashboardPage'
import ProfilePage from './components/ProfilePage'
import LoginCard from './components/LoginCard'
import RegisterForm from './components/RegisterForm'

function App() {
  const [activeSection, setActiveSection] = useState('dashboard')
  const [theme, setTheme] = useState('dark')
  const [user, setUser] = useState({
    name: 'user',
    email: 'user@example.com',
    password: '********',
  })

  return (
  activeSection === 'register' ? (
    <RegisterForm
      onRegister={() => setActiveSection('login')}
      onLogin={() => setActiveSection('login')}
    />
  ) :
    activeSection === 'login' ? (
    <LoginCard onLogin={() => setActiveSection('dashboard')} />
  ) : 
  activeSection === 'profile' ? (
    <ProfilePage
      onNavigate={setActiveSection}
      user={user}
      onUpdateUser={setUser}
    />
  ) : (
    <DashboardPage
      onNavigate={setActiveSection}
      theme={theme}
    />
  )
  )
}

export default App
