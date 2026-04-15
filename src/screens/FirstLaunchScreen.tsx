import React, {useState} from 'react';
import {
  View, Text, TouchableOpacity, StyleSheet,
  Platform, PermissionsAndroid, Alert,
} from 'react-native';
import ServiceModule from '../native/ServiceModule';

type Props = {onComplete: () => void};

export default function FirstLaunchScreen({onComplete}: Props) {
  const [step, setStep] = useState<'permissions' | 'battery'>('permissions');

  async function requestPermissions() {
    if (Platform.OS !== 'android') { setStep('battery'); return; }
    try {
      if (Platform.Version >= 33) {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
          {title: 'Notification Permission',
           message: 'KindnessDice needs this to show the service indicator.',
           buttonPositive: 'Allow', buttonNegative: 'Skip'},
        );
        if (granted === PermissionsAndroid.RESULTS.DENIED) {
          Alert.alert('Permission needed', 'Without this the service may not work reliably.');
        }
      }
      setStep('battery');
    } catch { setStep('battery'); }
  }

  function requestBatteryOptimization() {
    ServiceModule.requestIgnoreBatteryOptimizations();
    setTimeout(onComplete, 1000);
  }

  if (step === 'permissions') {
    return (
      <View style={styles.container}>
        <Text style={styles.emoji}>🎲</Text>
        <Text style={styles.title}>Welcome to{'\n'}Kindness Dice</Text>
        <Text style={styles.body}>
          Shake your phone for 5.5 seconds to receive a random act of kindness.{'\n\n'}First, we need a couple of permissions.
        </Text>
        <TouchableOpacity style={styles.button} onPress={requestPermissions}>
          <Text style={styles.buttonText}>Grant Permissions</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.emoji}>🔋</Text>
      <Text style={styles.title}>Battery Optimization</Text>
      <Text style={styles.body}>
        Tap below and select {'"'}Don{"\u2019"}t optimize{'"'} so Android won{"\u2019"}t stop the shake detector.
      </Text>
      <TouchableOpacity style={styles.button} onPress={requestBatteryOptimization}>
        <Text style={styles.buttonText}>Open Battery Settings</Text>
      </TouchableOpacity>
      <TouchableOpacity style={styles.skip} onPress={onComplete}>
        <Text style={styles.skipText}>Skip for now</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex:1, backgroundColor:'#FFF8EF', alignItems:'center', justifyContent:'center', paddingHorizontal:40},
  emoji: {fontSize:64, marginBottom:24},
  title: {fontSize:28, fontWeight:'300', color:'#5D3A1A', textAlign:'center', marginBottom:20, lineHeight:36},
  body: {fontSize:16, color:'#7B5C3A', textAlign:'center', lineHeight:24, marginBottom:40},
  button: {backgroundColor:'#C4722A', paddingHorizontal:40, paddingVertical:16, borderRadius:32},
  buttonText: {color:'#FFFFFF', fontSize:16, fontWeight:'600'},
  skip: {marginTop:20, padding:12},
  skipText: {color:'#C4A882', fontSize:14},
});
