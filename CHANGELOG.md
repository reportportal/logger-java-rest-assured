# Changelog

## [Unreleased]
### Fixed
- NullPointerExceptions in case of null values for different converters, by @HardNorth
### Changed
- Client version updated on [5.1.11](https://github.com/reportportal/client-java/releases/tag/5.1.11), by @HardNorth
- `new Date()` replaced with `Calendar.getInstance().getTime()`

## [5.1.5]
### Fixed
- Invalid content pretty-print crash, by @HardNorth

## [5.1.4]
### Fixed
- Empty Content-Type parse exception, by @HardNorth

## [5.1.3]
### Fixed
- Cookies indent when use with headers, by @HardNorth
### Changed
- All converters and prettiers moved into `com.epam.reportportal.restassured.support.Converters` class, by @HardNorth

## [5.1.2]
### Fixed
- Missed HTTP headers in case of GET requests, by @HardNorth

## [5.1.1]
### Added
- Example converters, by @HardNorth

## [5.1.0]
### Added
- Chain accessors for setters, by @HardNorth
### Fixed
- Logger crash on empty Content-Type HTTP header, by @HardNorth

## [5.0.1]
### Added
- URL converter function, by @HardNorth

## [5.0.0]
### Added
- Initial release of REST Assured logger, by @HardNorth

## [5.1.0-ALPHA-1]
