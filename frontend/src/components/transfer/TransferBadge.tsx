import { Box, Popover, Stack, Typography } from '@mui/material'
import { useId, useState, type MouseEvent, type ReactNode } from 'react'
import type { TransferMoveSummary } from '@/types/domain'
import { formatNumber } from '@/utils/format'

type TransferBadgeProps = {
  transfer: TransferMoveSummary
  /** Optional label shown before the T chip (e.g. club name). */
  children?: ReactNode
}

function formatFee(feeEur: number | null): string {
  if (feeEur == null) {
    return 'Fee undisclosed'
  }
  if (feeEur === 0) {
    return 'Free / undisclosed'
  }
  if (feeEur >= 1_000_000) {
    return `€${formatNumber(feeEur / 1_000_000, 1)}m`
  }
  return `€${formatNumber(feeEur, 0)}`
}

export function TransferBadge({ transfer, children }: TransferBadgeProps) {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null)
  const popoverId = useId()
  const open = Boolean(anchor)

  const handleOpen = (event: MouseEvent<HTMLElement>) => {
    event.preventDefault()
    event.stopPropagation()
    setAnchor(event.currentTarget)
  }

  const handleClose = () => {
    setAnchor(null)
  }

  return (
    <>
      <Box
        component="span"
        sx={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 0.75,
          verticalAlign: 'middle',
        }}
      >
        {children}
        <Box
          aria-describedby={open ? popoverId : undefined}
          aria-label="Transfer details"
          component="button"
          onClick={handleOpen}
          type="button"
          sx={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            minWidth: 22,
            height: 22,
            px: 0.5,
            borderRadius: 1,
            border: '1px solid',
            borderColor: 'primary.main',
            bgcolor: 'action.hover',
            color: 'primary.main',
            fontSize: 11,
            fontWeight: 700,
            lineHeight: 1,
            letterSpacing: 0.02,
            cursor: 'pointer',
            fontFamily: 'inherit',
          }}
        >
          T
        </Box>
      </Box>
      <Popover
        anchorEl={anchor}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        id={popoverId}
        onClose={handleClose}
        open={open}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      >
        <Stack spacing={0.75} sx={{ p: 1.5, minWidth: 220, maxWidth: 300 }}>
          <Typography color="primary.main" variant="caption">
            Transfer{transfer.seasonLabel ? ` · ${transfer.seasonLabel}` : ''}
          </Typography>
          <Typography variant="body2">
            {transfer.fromClubName ?? 'Free agent'} → {transfer.toClubName ?? 'Unknown'}
          </Typography>
          <Typography color="text.secondary" variant="body2">
            {formatFee(transfer.feeEur)}
            {transfer.transferDate ? ` · ${transfer.transferDate}` : ''}
          </Typography>
        </Stack>
      </Popover>
    </>
  )
}
