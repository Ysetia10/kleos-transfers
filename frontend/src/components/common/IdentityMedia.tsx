import { Avatar, Box, type SxProps, type Theme } from '@mui/material'
import { useEffect, useState } from 'react'

function initialsFrom(label: string): string {
  const parts = label.trim().split(/\s+/).filter(Boolean)
  if (parts.length === 0) {
    return '?'
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase()
  }
  return `${parts[0][0] ?? ''}${parts[1][0] ?? ''}`.toUpperCase()
}

interface IdentityMediaProps {
  label: string
  imageUrl?: string | null
  size?: number
  rounded?: 'circle' | 'soft'
  sx?: SxProps<Theme>
}

/** Photo/crest with initials fallback when URL is missing or fails to load. */
export function IdentityMedia({
  label,
  imageUrl,
  size = 40,
  rounded = 'circle',
  sx,
}: IdentityMediaProps) {
  const [failed, setFailed] = useState(false)
  useEffect(() => {
    setFailed(false)
  }, [imageUrl])
  const showImage = Boolean(imageUrl) && !failed
  const radius = rounded === 'circle' ? '50%' : 2

  if (showImage) {
    return (
      <Box
        alt={label}
        component="img"
        loading="lazy"
        onError={() => setFailed(true)}
        referrerPolicy="no-referrer"
        src={imageUrl ?? undefined}
        sx={{
          width: size,
          height: size,
          objectFit: 'contain',
          objectPosition: 'center',
          borderRadius: radius,
          display: 'block',
          flexShrink: 0,
          backgroundColor: 'transparent',
          ...(sx ?? {}),
        }}
      />
    )
  }

  return (
    <Avatar
      alt={label}
      sx={{
        width: size,
        height: size,
        borderRadius: radius,
        fontSize: size * 0.36,
        fontWeight: 700,
        bgcolor: 'primary.main',
        color: 'primary.contrastText',
        flexShrink: 0,
        ...(sx ?? {}),
      }}
      variant={rounded === 'circle' ? 'circular' : 'rounded'}
    >
      {initialsFrom(label)}
    </Avatar>
  )
}

interface IdentityMediaWithCreditProps extends IdentityMediaProps {
  attribution?: string | null
  license?: string | null
}

/** Detail-page media block with optional Wikimedia-style credit line. */
export function IdentityMediaWithCredit({
  attribution,
  license,
  ...media
}: IdentityMediaWithCreditProps) {
  const credit = [attribution, license].filter(Boolean).join(' · ')

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.75, alignItems: 'flex-start' }}>
      <IdentityMedia {...media} />
      {credit ? (
        <Box
          component="span"
          sx={{
            color: 'text.secondary',
            fontSize: 11,
            lineHeight: 1.35,
            maxWidth: 280,
          }}
        >
          {credit}
        </Box>
      ) : null}
    </Box>
  )
}
