import {NativeModules} from 'react-native';

const {ServiceModule} = NativeModules as {
  ServiceModule: {
    startService(): void;
    stopService(): void;
    isServiceRunning(): Promise<boolean>;
    requestIgnoreBatteryOptimizations(): void;
  };
};

export default ServiceModule;
