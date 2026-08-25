import { IconButton, Tooltip } from '@mui/material'
import { useColorMode } from '@/context/ColorModeContext'

function SunIcon() {
  return (
    <svg fill="none" height="18" viewBox="0 0 24 24" width="18" xmlns="http://www.w3.org/2000/svg">
      <circle cx="12" cy="12" r="4" stroke="currentColor" strokeWidth="1.8" />
      <path
        d="M12 3v2.2M12 18.8V21M3 12h2.2M18.8 12H21M5.6 5.6l1.6 1.6M16.8 16.8l1.6 1.6M5.6 18.4l1.6-1.6M16.8 7.2l1.6-1.6"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}

function MoonIcon() {
  return (
    <svg fill="none" height="18" viewBox="0 0 24 24" width="18" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M16.5 3.5A8.5 8.5 0 1 0 20.5 14.2 6.8 6.8 0 0 1 16.5 3.5Z"
        stroke="currentColor"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  )
}

export function ThemeToggle() {
  const { mode, toggleMode } = useColorMode()
  const next = mode === 'dark' ? 'light' : 'dark'

  return (
    <Tooltip title={`Switch to ${next} theme`}>
      <IconButton
        aria-label={`Switch to ${next} theme`}
        color="inherit"
        onClick={toggleMode}
        size="small"
        sx={{
          border: (theme) => `1px solid ${theme.palette.divider}`,
          borderRadius: 999,
          color: 'text.secondary',
          width: 44,
          height: 44,
          '&:hover': { color: 'text.primary', borderColor: 'primary.main' },
        }}
      >
        {mode === 'dark' ? <SunIcon /> : <MoonIcon />}
      </IconButton>
    </Tooltip>
  )
}
