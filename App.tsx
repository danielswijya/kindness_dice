import React, {useEffect, useState} from 'react';
import {StatusBar} from 'react-native';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import AsyncStorage from '@react-native-async-storage/async-storage';
import FirstLaunchScreen from './src/screens/FirstLaunchScreen';
import SettingsScreen from './src/screens/SettingsScreen';
import ServiceModule from './src/native/ServiceModule';

const FIRST_LAUNCH_KEY = '@kindness_dice_onboarded';

export default function App() {
  const [onboarded, setOnboarded] = useState<boolean | null>(null);

  useEffect(() => {
    AsyncStorage.getItem(FIRST_LAUNCH_KEY).then(value => setOnboarded(value === 'true'));
  }, []);

  async function handleOnboardingComplete() {
    await AsyncStorage.setItem(FIRST_LAUNCH_KEY, 'true');
    setOnboarded(true);
    ServiceModule.startService();
  }

  if (onboarded === null) return null;

  return (
    <SafeAreaProvider>
      <StatusBar barStyle="dark-content" backgroundColor="#FFF8EF" />
      {onboarded ? <SettingsScreen /> : <FirstLaunchScreen onComplete={handleOnboardingComplete} />}
    </SafeAreaProvider>
  );
}
