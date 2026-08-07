import type { Components, Theme } from '@mui/material/styles'

export const components: Components<Theme> = {
  MuiCssBaseline: {
    styleOverrides: {
      '@keyframes kleos-rise': {
        from: { opacity: 0, transform: 'translateY(14px)' },
        to: { opacity: 1, transform: 'translateY(0)' },
      },
      '@keyframes kleos-mark-in': {
        '0%': { opacity: 0, transform: 'scale(0.86) rotate(-8deg)' },
        '100%': { opacity: 1, transform: 'scale(1) rotate(0deg)' },
      },
      body: {
        backgroundImage: `
          linear-gradient(180deg, rgba(238, 242, 238, 0.92) 0%, rgba(238, 242, 238, 0.97) 40%, rgba(238, 242, 238, 1) 100%),
          repeating-linear-gradient(
            90deg,
            transparent 0,
            transparent 47px,
            rgba(22, 53, 40, 0.045) 47px,
            rgba(22, 53, 40, 0.045) 48px
          ),
          repeating-linear-gradient(
            0deg,
            transparent 0,
            transparent 47px,
            rgba(22, 53, 40, 0.035) 47px,
            rgba(22, 53, 40, 0.035) 48px
          )
        `,
        backgroundAttachment: 'fixed',
      },
      '::selection': {
        backgroundColor: 'rgba(196, 154, 60, 0.35)',
      },
    },
  },
  MuiAppBar: {
    defaultProps: {
      elevation: 0,
    },
    styleOverrides: {
      root: ({ theme }) => ({
        backgroundColor: 'rgba(251, 252, 251, 0.86)',
        backdropFilter: 'blur(12px)',
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
        borderRadius: theme.shape.borderRadius,
        padding: theme.spacing(1, 2),
        transition: 'transform 160ms ease, box-shadow 160ms ease, background-color 160ms ease',
        '&:hover': {
          transform: 'translateY(-1px)',
        },
        '&:active': {
          transform: 'translateY(0)',
        },
        ...(ownerState.variant === 'contained' && ownerState.color === 'primary'
          ? {
              boxShadow: `0 8px 20px ${theme.palette.pitch.mist}`,
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
        borderRadius: theme.shape.borderRadius,
        backgroundImage: 'none',
        border: `1px solid ${theme.palette.divider}`,
      }),
    },
  },
  MuiTableRow: {
    styleOverrides: {
      root: ({ theme }) => ({
        transition: 'background-color 140ms ease',
        '&.MuiTableRow-hover:hover': {
          backgroundColor: theme.palette.pitch.mist,
        },
      }),
    },
  },
  MuiTableHead: {
    styleOverrides: {
      root: ({ theme }) => ({
        '& .MuiTableCell-head': {
          fontFamily: '"Barlow Condensed", "Arial Narrow", sans-serif',
          fontWeight: 600,
          letterSpacing: '0.06em',
          textTransform: 'uppercase',
          color: theme.palette.text.secondary,
          borderBottomColor: theme.palette.divider,
        },
      }),
    },
  },
  MuiOutlinedInput: {
    styleOverrides: {
      root: ({ theme }) => ({
        backgroundColor: theme.palette.background.paper,
        transition: 'box-shadow 160ms ease',
        '&.Mui-focused': {
          boxShadow: `0 0 0 3px ${theme.palette.pitch.mist}`,
        },
      }),
    },
  },
}
