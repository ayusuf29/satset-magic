import React from 'react';
import { interpolate, spring, useCurrentFrame, useVideoConfig } from 'remotion';
import { THEME } from '../types';

export const Scene5_MergeAndRelease: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();

  const titleFade = interpolate(frame, [0, 20], [0, 1]);
  const progress1 = spring({ frame: frame - 20, fps, config: { damping: 12 } });
  const progress2 = spring({ frame: frame - 60, fps, config: { damping: 12 } });
  const progress3 = spring({ frame: frame - 100, fps, config: { damping: 12 } });

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
        padding: '50px 80px',
        textAlign: 'center',
      }}
    >
      <div style={{ opacity: titleFade }}>
        <div style={{ fontSize: 18, letterSpacing: 4, fontWeight: 800, color: THEME.success }}>
          CI/CD PIPELINE EXECUTION
        </div>
        <h2 style={{ fontSize: 54, fontWeight: 900, color: THEME.text, margin: '8px 0 20px 0' }}>
          Merge ke Main &amp; Pre-Production Gate
        </h2>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: 32,
          marginTop: 40,
          width: '100%',
          maxWidth: 1350,
        }}
      >
        {/* Step 1: Git Merge to Main */}
        <div
          style={{
            transform: `scale(${progress1})`,
            backgroundColor: THEME.surface,
            borderRadius: 24,
            padding: 36,
            border: `2px solid ${THEME.primary}`,
            textAlign: 'left',
          }}
        >
          <div style={{ fontSize: 44, marginBottom: 12 }}>🌿</div>
          <div style={{ fontSize: 24, fontWeight: 800, color: '#60a5fa' }}>Git Merge Commit</div>
          <div style={{ fontSize: 16, color: THEME.textMuted, marginTop: 8 }}>
            Merge commit <code>#a4b1c2</code> resmi masuk ke protected branch <code>main</code> setelah otorisasi
            manusia.
          </div>
          <div
            style={{
              marginTop: 20,
              padding: '8px 16px',
              backgroundColor: 'rgba(59,130,246,0.15)',
              color: '#93c5fd',
              borderRadius: 8,
              fontSize: 14,
              fontWeight: 700,
              display: 'inline-block',
            }}
          >
            ✓ Branch Synchronized
          </div>
        </div>

        {/* Step 2: DAST & k6 Stress Test */}
        <div
          style={{
            transform: `scale(${progress2})`,
            backgroundColor: THEME.surface,
            borderRadius: 24,
            padding: 36,
            border: `2px solid ${THEME.indigo}`,
            textAlign: 'left',
          }}
        >
          <div style={{ fontSize: 44, marginBottom: 12 }}>🧪</div>
          <div style={{ fontSize: 24, fontWeight: 800, color: '#818cf8' }}>DAST &amp; Stress Audit</div>
          <div style={{ fontSize: 16, color: THEME.textMuted, marginTop: 8 }}>
            OWASP ZAP Dynamic Fuzzing (0 High/Crit) &amp; Grafana k6 Load Test (P95 &lt; 85ms).
          </div>
          <div
            style={{
              marginTop: 20,
              padding: '8px 16px',
              backgroundColor: 'rgba(99,102,241,0.15)',
              color: '#c7d2fe',
              borderRadius: 8,
              fontSize: 14,
              fontWeight: 700,
              display: 'inline-block',
            }}
          >
            ✓ Staging Verified
          </div>
        </div>

        {/* Step 3: Human Production Gate */}
        <div
          style={{
            transform: `scale(${progress3})`,
            backgroundColor: THEME.surface,
            borderRadius: 24,
            padding: 36,
            border: `2px solid ${THEME.warning}`,
            textAlign: 'left',
          }}
        >
          <div style={{ fontSize: 44, marginBottom: 12 }}>🏁</div>
          <div style={{ fontSize: 24, fontWeight: 800, color: '#fb923c' }}>Production Gate (HITL 3)</div>
          <div style={{ fontSize: 16, color: THEME.textMuted, marginTop: 8 }}>
            Tanda tangan final sebelum deployment ke server live production (Phase 9 Released).
          </div>
          <div
            style={{
              marginTop: 20,
              padding: '8px 16px',
              backgroundColor: 'rgba(249,115,22,0.15)',
              color: '#fdba74',
              borderRadius: 8,
              fontSize: 14,
              fontWeight: 700,
              display: 'inline-block',
            }}
          >
            🔒 Final Human Sign-off
          </div>
        </div>
      </div>
    </div>
  );
};
