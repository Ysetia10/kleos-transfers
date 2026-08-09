import { Box, Tooltip } from '@mui/material'

type NewBadgeProps = {
  label?: string
}

/** Compact marker for a manager's first season at a club. */
export function NewBadge({ label = 'First season at this club' }: NewBadgeProps) {
  return (
    <Tooltip title={label}>
      <Box
        component="span"
        sx={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          ml: 0.75,
          px: 0.6,
          height: 20,
          borderRadius: 1,
          border: '1px solid',
          borderColor: 'success.main',
          color: 'success.main',
          fontSize: 10,
          fontWeight: 700,
          letterSpacing: 0.04,
          textTransform: 'uppercase',
          verticalAlign: 'middle',
        }}
      >
        New
      </Box>
    </Tooltip>
  )
}
