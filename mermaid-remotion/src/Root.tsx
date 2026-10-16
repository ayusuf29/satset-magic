import React from 'react';
import { Composition } from 'remotion';
import { MainComposition } from './MainComposition';
import { VIDEO_CONFIG } from './types';

export const RemotionRoot: React.FC = () => {
  return (
    <Composition
      id="SDLCWorkflowExplainer"
      component={MainComposition}
      durationInFrames={VIDEO_CONFIG.totalFrames}
      fps={VIDEO_CONFIG.fps}
      width={VIDEO_CONFIG.width}
      height={VIDEO_CONFIG.height}
      defaultProps={{}}
    />
  );
};
