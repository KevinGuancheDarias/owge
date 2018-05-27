// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

module.exports = function (config) {
  config.set({
    webpack: {
      node: {
        fs: 'empty'
      }
    },
    basePath: '',
    preprocessors: {
      './src/*.less': ['less']
    },
    frameworks: ['jasmine', '@angular/cli'],
    plugins: [
      require('karma-less-preprocessor'),
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage-istanbul-reporter'),
      require('@angular/cli/plugins/karma')
    ],
    client: {
      clearContext: false // leave Jasmine Spec Runner output visible in browser
    },
    coverageIstanbulReporter: {
      reports: ['html', 'lcovonly'],
      fixWebpackSourcePaths: true
    },
    angularCli: {
      environment: 'dev'
    },
    reporters: ['progress', 'kjhtml'],
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers: ['Chrome'],
    singleRun: false,
    files: [
      "./node_modules/bootstrap/dist/css/bootstrap.css",
      "./node_modules/font-awesome/css/font-awesome.css",
      "./src/common_include.less",
      "./src/styles.less",
      "./src/forms.less",
      "./src/test-style.less"
    ],
    lessPreprocessor: {
      options: {
        paths: ['src'],
        save: false,
      }
    }
  });
};
