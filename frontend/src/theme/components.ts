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
      html: {
        overflowX: 'clip',
      },
      body: {
        backgroundImage: 'none',
        fontFamily: 'Arial, Helvetica, sans-serif',
        overflowX: 'clip',
        maxWidth: '100vw',
      },
      img: {
        maxWidth: '100%',
        height: 'auto',
      },
      'b, strong': {
        fontWeight: 500,
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
          theme.palette.mode === 'dark' ? 'rgba(11, 14, 20, 0.88)' : 'rgba(255, 255, 255, 0.92)',
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
        minHeight: 44,
        textTransform: 'none',
        fontWeight: 500,
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
      }),
    },
  },
  MuiTableContainer: {
    styleOverrides: {
      root: {
        maxWidth: '100%',
      },
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
        ...theme.typography.label,
        color: theme.palette.text.secondary,
        textTransform: 'uppercase',
        letterSpacing: '0.04em',
        borderBottomColor: theme.palette.divider,
      }),
      body: ({ theme }) => ({
        ...theme.typography.normal,
        borderBottomColor: theme.palette.divider,
      }),
    },
  },
  MuiChip: {
    styleOverrides: {
      root: {
        borderRadius: 8,
        fontWeight: 500,
      },
    },
  },
  MuiLink: {
    styleOverrides: {
      root: {
        fontWeight: 500,
      },
    },
  },
}
