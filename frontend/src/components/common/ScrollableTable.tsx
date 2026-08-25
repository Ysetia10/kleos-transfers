import { TableContainer, type TableContainerProps } from '@mui/material'
import type { PropsWithChildren } from 'react'

type ScrollableTableProps = PropsWithChildren<TableContainerProps> & {
  /** Minimum table width before horizontal scroll kicks in. */
  minWidth?: number
}

/**
 * Horizontal scroll for data tables on narrow viewports.
 * Prefer this over clipping with overflow:hidden alone.
 */
export function ScrollableTable({
  children,
  minWidth = 560,
  sx,
  ...rest
}: ScrollableTableProps) {
  return (
    <TableContainer
      {...rest}
      sx={[
        {
          overflowX: 'auto',
          WebkitOverflowScrolling: 'touch',
          maxWidth: '100%',
          '& .MuiTable-root': {
            minWidth,
          },
        },
        ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
      ]}
    >
      {children}
    </TableContainer>
  )
}
