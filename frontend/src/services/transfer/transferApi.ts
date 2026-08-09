import { httpClient } from '@/services/api/httpClient'
import type { SpringPage } from '@/types/domain'

export type TransferStatus = 'COMPLETED' | 'ANNOUNCED' | 'RUMOURED'
export type TransferType = 'PERMANENT' | 'LOAN' | 'FREE' | 'LOAN_RETURN'

export interface Transfer {
  id: string
  playerId: string
  playerName: string
  fromClubId: string | null
  fromClubName: string | null
  toClubId: string | null
  toClubName: string | null
  seasonId: string
  seasonLabel: string
  transferDate: string
  feeEur: number | null
  type: TransferType
  status: TransferStatus
  source: string | null
  notes: string | null
  createdAt: string
  updatedAt: string
}

export async function listTransfers(
  page = 0,
  size = 10,
  status?: TransferStatus,
): Promise<SpringPage<Transfer>> {
  const { data } = await httpClient.get<SpringPage<Transfer>>('/api/v1/transfers', {
    params: {
      page,
      size,
      sort: 'transferDate,desc',
      ...(status ? { status } : {}),
    },
  })
  return data
}
