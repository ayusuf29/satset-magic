import React from 'react';
import { interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { THEME } from '../types';

export const Scene4_TelegramHITL: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const phoneTranslateY = interpolate(frame, [0, 25], [600, 0], { extrapolateRight: 'clamp' });
  const isApproved = frame >= 180;
  const buttonClickScale = frame > 165 && frame < 185 ? 0.94 : 1.0;
  const badgePulse = isApproved ? spring({ frame: frame - 180, fps, config: { damping: 12 } }) : 1;

  return (
    <div
      style={{
        display: 'flex',
        height: '100%',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '60px 100px',
        gap: 80,
      }}
    >
      {/* Left Text Explanation */}
      <div style={{ flex: 1, textAlign: 'left' }}>
        <div
          style={{
            color: THEME.warning,
            fontWeight: 800,
            fontSize: 20,
            letterSpacing: 3,
            textTransform: 'uppercase',
            marginBottom: 12,
          }}
        >
          REMOTE OPERATOR INTERFACE
        </div>
        <h2 style={{ fontSize: 52, fontWeight: 900, lineHeight: 1.15, color: THEME.text, margin: 0 }}>
          Eksekusi HITL Cepat via Telegram Bot
        </h2>
        <p style={{ fontSize: 22, color: THEME.textMuted, marginTop: 24, lineHeight: 1.6 }}>
          Tech Lead menerima kartu notifikasi interaktif secara langsung di Telegram. Dilengkapi ringkasan metrik audit
          Council, tautan diff, dan tombol callback satu sentuhan untuk merilis ke branch <code>main</code>.
        </p>

        <div style={{ marginTop: 32, display: 'flex', gap: 16 }}>
          <div
            style={{
              padding: '12px 20px',
              backgroundColor: THEME.surface,
              borderRadius: 12,
              border: `1px solid ${THEME.border}`,
              fontSize: 16,
              color: '#cbd5e1',
            }}
          >
            🔒 Whitelist RBAC Verification
          </div>
          <div
            style={{
              padding: '12px 20px',
              backgroundColor: THEME.surface,
              borderRadius: 12,
              border: `1px solid ${THEME.border}`,
              fontSize: 16,
              color: '#cbd5e1',
            }}
          >
            ⚡ In-Place Message Update
          </div>
        </div>
      </div>

      {/* Right Smartphone UI Mockup */}
      <div
        style={{
          transform: `translateY(${phoneTranslateY}px)`,
          width: 440,
          height: 840,
          backgroundColor: '#17212b',
          borderRadius: 52,
          border: '12px solid #242f3d',
          boxShadow: '0 30px 90px rgba(0,0,0,0.85)',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          position: 'relative',
        }}
      >
        {/* Notch / Speaker */}
        <div
          style={{
            height: 54,
            backgroundColor: '#242f3d',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#94a3b8',
            fontSize: 14,
            fontWeight: 700,
          }}
        >
          📱 @SatsetMagicBot
        </div>

        {/* Telegram Chat Area */}
        <div style={{ padding: 20, flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
          <div
            style={{
              backgroundColor: '#1e2c3a',
              borderRadius: 20,
              padding: 24,
              color: '#f8fafc',
              borderLeft: `4px solid ${isApproved ? THEME.success : THEME.warning}`,
              boxShadow: '0 10px 25px rgba(0,0,0,0.3)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
              <span style={{ fontSize: 20 }}>{isApproved ? '✅' : '⚠️'}</span>
              <span style={{ fontWeight: 800, fontSize: 18, color: isApproved ? '#34d399' : '#fb923c' }}>
                {isApproved ? 'PR MERGED TO MAIN' : 'HUMAN APPROVAL REQUIRED'}
              </span>
            </div>

            <div style={{ fontSize: 14, color: '#cbd5e1', lineHeight: 1.6 }}>
              <b>Workflow:</b> <code>wf-98b7c4a1</code><br />
              <b>Task:</b> Idempotent Payment Webhook<br />
              <b>Branch:</b> <code>feat/*</code> ➔ <code>main</code><br />
              <b>Council:</b> 3/3 PASS (Coverage 88.4%)
            </div>

            {/* Interactive Inline Buttons */}
            <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 10 }}>
              <div
                style={{
                  backgroundColor: isApproved ? '#059669' : '#2563eb',
                  color: '#ffffff',
                  padding: '14px 18px',
                  borderRadius: 12,
                  textAlign: 'center',
                  fontWeight: 700,
                  fontSize: 15,
                  transform: `scale(${isApproved ? badgePulse : buttonClickScale})`,
                  cursor: 'pointer',
                  boxShadow: isApproved ? '0 8px 20px rgba(5,150,105,0.4)' : 'none',
                }}
              >
                {isApproved ? '✓ APPROVED & MERGED BY @LEAD' : '✅ APPROVE & MERGE TO MAIN'}
              </div>

              {!isApproved && (
                <div
                  style={{
                    backgroundColor: '#374151',
                    color: '#f87171',
                    padding: '12px 18px',
                    borderRadius: 12,
                    textAlign: 'center',
                    fontWeight: 600,
                    fontSize: 14,
                  }}
                >
                  ❌ REJECT WITH NOTE
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
