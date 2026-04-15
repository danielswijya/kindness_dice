import React, {useEffect, useState} from 'react';
import {View, Text, Switch, StyleSheet, ActivityIndicator} from 'react-native';
import ServiceModule from '../native/ServiceModule';

export default function SettingsScreen() {
  const [isEnabled, setIsEnabled] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    ServiceModule.isServiceRunning()
      .then(running => { setIsEnabled(running); setLoading(false); })
      .catch(() => setLoading(false));
  }, []);

  function toggleService(value: boolean) {
    setIsEnabled(value);
    if (value) { ServiceModule.startService(); }
    else { ServiceModule.stopService(); }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.emoji}>🎲</Text>
      <Text style={styles.title}>Kindness Dice</Text>
      <Text style={styles.subtitle}>Shake for 5.5 seconds to roll a kindness.</Text>
      <View style={styles.card}>
        <View style={styles.row}>
          <View style={styles.rowText}>
            <Text style={styles.rowLabel}>Kindness Detector</Text>
            <Text style={styles.rowSub}>{isEnabled ? 'Running in background' : 'Tap to enable'}</Text>
          </View>
          {loading
            ? <ActivityIndicator color="#C4722A" />
            : <Switch value={isEnabled} onValueChange={toggleService}
                trackColor={{false:'#D4C5B2', true:'#C4722A'}} thumbColor="#FFFFFF" />}
        </View>
      </View>
      <Text style={styles.hint}>Starts automatically on boot.</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex:1, backgroundColor:'#FFF8EF', alignItems:'center', justifyContent:'center', paddingHorizontal:32},
  emoji: {fontSize:56, marginBottom:16},
  title: {fontSize:30, fontWeight:'300', color:'#5D3A1A', marginBottom:8},
  subtitle: {fontSize:15, color:'#7B5C3A', textAlign:'center', marginBottom:48, lineHeight:22},
  card: {width:'100%', backgroundColor:'#FFFFFF', borderRadius:16, paddingHorizontal:20, paddingVertical:16, elevation:3, marginBottom:24},
  row: {flexDirection:'row', alignItems:'center', justifyContent:'space-between'},
  rowText: {flex:1, marginRight:16},
  rowLabel: {fontSize:16, color:'#5D3A1A', fontWeight:'500'},
  rowSub: {fontSize:13, color:'#9E8074', marginTop:2},
  hint: {fontSize:13, color:'#C4A882', textAlign:'center', lineHeight:20},
});
