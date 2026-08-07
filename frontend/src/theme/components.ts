import type { Components, Theme } from '@mui/material/styles'

export const components: Components<Theme> = {
  MuiCssBaseline: {
    styleOverrides: {
      '@keyframes kleos-rise': {
        from: { opacity: 0, transform: 'translateY(12px)' },
        to: { opacity: 1, transform: 'translateY(0)' },
      },
      '@keyframes kleos-mark-in': {
        '0%': { opacity: 0, transform: 'scale(0.86) rotate(-8deg)' },
        '100%': { opacity: 1, transform: 'scale(1) rotate(0deg)' },
      },
      body: {
        backgroundImage: 'none',
      },
      '::selection': {
        backgroundColor: 'rgba(59, 130, 246, 0.35)',
      },
    },
  },
  MuiAppBar: {
    defaultProps: {
      elevation: 0,
    },
    styleOverrides: {
      root: ({ theme }) => ({
        backgroundColor:
          theme.palette.mode === 'dark' ? 'rgba(11, 14, 20, 0.88)' : 'rgba(244, 246, 248, 0.88)',
        backdropFilter: 'blur(14px)',
        borderBottom: `1px solid ${theme.palette.divider}`,
        color: theme.palette.text.primary,
      }),
    },
  },
  MuiButton: {
    defaultProps: {
      disableElevation: true,
    },
    styleOverrides: {
      root: ({ theme, ownerState }) => ({
        borderRadius: 10,
        padding: theme.spacing(1, 2),
        textTransform: 'none',
        fontWeight: 600,
        transition: 'transform 160ms ease, box-shadow 160ms ease, background-color 160ms ease',
        '&:hover': {
          transform: 'translateY(-1px)',
        },
        '&:active': {
          transform: 'translateY(0)',
        },
        ...(ownerState.variant === 'contained' && ownerState.color === 'primary'
          ? {
              boxShadow: `0 10px 24px ${theme.palette.pitch.mist}`,
            }
          : {}),
        ...(ownerState.variant === 'contained' && ownerState.color === 'accent'
          ? {
              backgroundColor: theme.palette.accent.main,
              color: theme.palette.accent.contrastText,
              '&:hover': {
                backgroundColor: theme.palette.accent.dark,
                transform: 'translateY(-1px)',
              },
            }
          : {}),
      }),
    },
  },
  MuiPaper: {
    styleOverrides: {
      root: ({ theme }) => ({
        backgroundImage: 'none',
        border: `1px solid ${theme.palette.divider}`,
      }),
    },
  },
  MuiCard: {
    styleOverrides: {
      root: ({ theme }) => ({
        backgroundImage: 'none',
        border: `1px solid ${theme.palette.divider}`,
        borderRadius: 16,
        boxShadow: 'none',
      }),
    },
  },
  MuiOutlinedInput: {
    styleOverrides: {
      root: ({ theme }) => ({
        borderRadius: 12,
        backgroundColor:
          theme.palette.mode === 'dark' ? 'rgba(20, 26, 34, 0.9)' : theme.palette.background.paper,
      }),
    },
  },
  MuiTableCell: {
    styleOverrides: {
      head: ({ theme }) => ({
        color: theme.palette.text.secondary,
        fontSize: '0.75rem',
        fontWeight: 600,
        letterSpacing: '0.04em',
        textTransform: 'uppercase',
        borderBottomColor: theme.palette.divider,
      }),
      body: ({ theme }) => ({
        borderBottomColor: theme.palette.divider,
      }),
    },
  },
  MuiChip: {
    styleOverrides: {
      root: {
        borderRadius: 8,
        fontWeight: 600,
      },
    },
  },
  MuiLink: {
    styleOverrides: {
      root: {
        fontWeight: 600,
      },
    },
  },
}
