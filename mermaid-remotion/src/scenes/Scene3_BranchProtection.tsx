import React from 'react';
import { interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { THEME } from '../types';

export const Scene3_BranchProtection: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const titleFade = interpolate(frame, [0, 20], [0, 1]);
  const lockSpring = spring({ frame: frame - 25, fps, config: { damping: 10 } });
  const pulseAlert = Math.sin(frame / 6) * 0.05 + 1;

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        padding: '50px 100px',
        textAlign: 'center',
      }}
    >
      <div style={{ opacity: titleFade }}>
        <div
          style={{
            fontSize: 18,
            letterSpacing: 4,
            fontWeight: 800,
            color: THEME.warning,
            textTransform: 'uppercase',
            marginBottom: 10,
          }}
        >
          PHASE 6 GOVERNANCE GAP RESOLVED
        </div>
        <h2 style={{ fontSize: 56, fontWeight: 900, color: THEME.text, margin: 0 }}>
          Protected Branch &amp; Mandatory HITL
        </h2>
        <p style={{ fontSize: 24, color: THEME.textMuted, maxWidth: 900, marginTop: 16 }}>
          Konsensus agen otomatis saja tidak cukup. Hanya pimpinan teknis manusia (Human Tech Lead) yang memiliki
          otoritas untuk menggabungkan kode ke branch utama (<code>main</code>).
        </p>
      </div>

      {/* Visual PR & Protection Lock Card */}
      <div
        style={{
          marginTop: 48,
          display: 'flex',
          alignItems: 'center',
          gap: 40,
          backgroundColor: THEME.surface,
          border: '2px solid rgba(244,63,94,0.4)',
          borderRadius: 32,
          padding: '40px 60px',
          maxWidth: 1100,
          boxShadow: '0 25px 60px rgba(244,63,94,0.15)',
        }}
      >
        {/* Animated Lock Icon */}
        <div
          style={{
            transform: `scale(${lockSpring * pulseAlert})`,
            fontSize: 90,
            backgroundColor: 'rgba(244,63,94,0.15)',
            border: '3px solid #f43f5e',
            borderRadius: '50%',
            width: 140,
            height: 140,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          🔒
        </div>

        <div style={{ textAlign: 'left', flex: 1 }}>
          <div
            style={{
              display: 'inline-block',
              backgroundColor: '#881337',
              color: '#ffe4e6',
              padding: '6px 16px',
              borderRadius: 8,
              fontSize: 14,
              fontWeight: 800,
              letterSpacing: 1,
              marginBottom: 12,
            }}
          >
            RULE: BOTS STRICTLY FORBIDDEN FROM MERGING TO MAIN
          </div>
          <h3 style={{ fontSize: 32, fontWeight: 800, color: '#f8fafc', margin: 0 }}>
            Pull Request #42: <code>feat/order-idempotency</code> ➔ <code>main</code>
          </h3>
          <p style={{ fontSize: 18, color: '#cbd5e1', marginTop: 12, lineHeight: 1.5 }}>
            Status Triad Council: <b>3/3 PASS</b>. Workflow otomatis beralih ke state{' '}
            <code style={{ color: '#fb923c' }}>PHASE_6_WAITING_HUMAN_CODE_REVIEW_AND_MERGE</code>.
          </p>
        </div>
      </div>
    </div>
  );
};
