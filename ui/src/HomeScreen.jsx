import React from 'react'
import ReactDOM from 'react-dom'
import { Table, Card } from 'reactstrap'
import { Tabs, Tab} from 'react-bootstrap'
import SemiCircleProgressBar from 'react-progressbar-semicircle'
import { FadeLoader } from 'react-spinners'
import { toast, Slide } from 'react-toastify'
import 'react-toastify/dist/ReactToastify.css'
import styles, {ProgressBarDiameter, TextColor} from './Styles.jsx'
import injectSheet from 'react-jss'
import { webProtocol } from './App.jsx';


const PROGRESS_BAR_1 = '#00c108'
const PROGRESS_BAR_2 = '#fff27d'
const PROGRESS_BAR_3 = '#ffac7d'
const PROGRESS_BAR_4 = '#ff4c4c'
const PROGRESS_BAR_5 = '#941818'

const REFRESH_TIME = 1 * 60 * 1000 // 1 minute expressed in mS


export default class HomeScreen extends React.Component {
    
    constructor() {
        super()
        
        this.state = {
            startSpinner: false,
        }

        this.refreshUITimer = undefined
        this.highUsageSecondsPerDay = 12 * 60 * 60

        this.subscribeForHome = this.subscribeForHome.bind(this)
        this.formatTime = this.formatTime.bind(this)
        this.startSpinner = this.startSpinner.bind(this)
        this.stopSpinnerTimer = this.stopSpinnerTimer.bind(this)
        this.viewIsFocused = this.viewIsFocused.bind(this)
        this.viewIsUnfocused = this.viewIsUnfocused.bind(this)
    }

    componentDidMount() {
        // provide context to the browser running this script so the browser
        // can inject javascript to commuicate with us.
        window.app = this

        // don't allow scrolling outside of specific scrollable views.
        document.body.style.overflow = 'hidden'

        // after 500mS of blank screen if the page has not loaded we will start a wiating spinnter
        this.spinnerDelayInterval = setTimeout(this.startSpinner, 500)

        try {
            // we need to get the OAuth token for the user owned by the Sendal mobile application.
            // the request is sent into the mobile app's execution environment.
            // will result in the mobile app invoking the userAuthTokenUpdate callback function.
            window.ReactNativeWebView.postMessage(JSON.stringify({ event: "UserAuthTokenRequest" }))
        } catch (err) {
            // this is a path that allows debugging to insert a hard-coded for test (or in some cases no) user token
            console.log('The native context does not exist yet for authToken')
            this.subscribeForHome()
        }
    }

    componentWillUnmount() {
        document.body.style.overflow = 'unset'

        this.stopSpinnerTimer()

        if (typeof this.refreshUITimer != 'undefined') {
            clearTimeout(this.refreshUITimer)
            this.refreshUITimer = undefined
        }
    }

    userAuthTokenUpdate(receivedToken) {
        // this method is called by the mobile app in response to requests for UserAuthToken
        // The mobile app injects javascript via the window.app definition to run this method.
        this.state.userAuthToken = receivedToken // set immediatly so it's available to the API calls.
        this.setState({ userAuthToken: receivedToken })
        this.subscribeForHome()
    }

    subscribeForHome() {
        // we 'subscribe' to the 3PSS service through our IS server for 
        // the home summary, which drives the information we diplay.
        console.log("Subscribing to home")

        if (typeof window.ReactNativeWebView != 'undefined') {
            window.ReactNativeWebView.postMessage(JSON.stringify({ event: "SetHeaderTitle", headerTitle: 'Light Tracker' }))
        }

        fetch(webProtocol + document.location.hostname + ":" + document.location.port
            + "/v1/pss/" + this.props.applicationId
            + "/users/" + this.props.userId
            + "/homes/" + this.props.homeId
            + "/api/passthrough/usagesummary",
            {
                headers: { authorization: this.state.userAuthToken }
            })
        .then(response => {
            if (response.ok == true) {
                response.json().then(usageSummary => {

                    let dayUsageSummary = [...usageSummary]
                    dayUsageSummary.sort((a, b) => { return (b.usageDaySeconds == a.usageDaySeconds) ? a.deviceName.localeCompare(b.deviceName) : b.usageDaySeconds - a.usageDaySeconds })

                    let weekUsageSummary = [...usageSummary]
                    weekUsageSummary.sort((a, b) => { return (b.usageWeekSeconds == a.usageWeekSeconds) ? a.deviceName.localeCompare(b.deviceName) : b.usageWeekSeconds - a.usageWeekSeconds })

                    let monthUsageSummary = [...usageSummary]
                    monthUsageSummary.sort((a, b) => { return (b.usageMonthSeconds == a.usageMonthSeconds) ? a.deviceName.localeCompare(b.deviceName) : b.usageMonthSeconds - a.usageMonthSeconds })

                    this.setState({
                        dayUsageSummary: dayUsageSummary,
                        weekUsageSummary: weekUsageSummary,
                        monthUsageSummary: monthUsageSummary
                    })
                    this.refreshUITimer = setTimeout(this.subscribeForHome, REFRESH_TIME)
                })
            } else if (response.status == 401 ||
                response.status == 403) {
                console.log("Unauthorized access - " + response.status)
                // in the polled method used for PSS it's likely an app that's been backgrounded has an out of date token, so ask
                // the mobile app for a refresh.
                try {
                    // should be rate as the mobile app should refresh tokens well in advance of their expiration, but
                    // in case something goes wrong or if the app has been running for a long time (i.e. hours)
                    // will result in the mobile app invoking the userAuthTokenUpdate callback function.
                    window.ReactNativeWebView.postMessage(JSON.stringify({ event: "UserAuthTokenRefreshRequest" }))
                } catch (err) {
                    console.log('Unauthorized access - The native context does not exist yet for authToken')
                }
            } else {
                console.log("Failed to retrieve service info - " + response.status)
                toast("Error: Could not get service info, error " + response.status + ".  Please try again by tapping the back button.")
            }
        })
        .catch(error => {
            console.log("Error in retrieving service info - " + error)
            toast("Error: Could not get service info  " + error + ".  Please try again by tapping the back button.")
            this.refreshUITimer = setTimeout(this.subscribeForHome, REFRESH_TIME)
        })

        // in parallel we check to see if the IS server has updated in case we are on this
        // screen for an extended time (reloading should be a rare event)
        fetch(webProtocol + document.location.hostname + ":" + document.location.port
            + "/v1/pss/" + this.props.applicationId
            + "/servertimestamp")
        .then(response => {
            if (response.ok == true) {
                response.json().then(timestamp => {
                    let serverTimestamp = timestamp.serverTimestamp
                    console.log("Comparing timestamp (" + this.state.serverTimestamp + ") to (" + serverTimestamp + ")")

                    if (typeof this.state.serverTimestamp != 'undefined') {
                        if (this.state.serverTimestamp.localeCompare(serverTimestamp) != 0) {
                            console.log("Reloading due to server timestamp (" + this.state.serverTimestamp + ") is different than updated - (" + serverTimestamp + ")")
                            window.location.reload(true)
                        }
                    }

                    this.setState({ serverTimestamp: serverTimestamp })
                })
            } else {
                console.log("Error in servertimestamp response - " + response.status)
            }
        })
    }

    viewIsFocused() {
        // an injected method from the mobile app.  Tells us that the view
        // running this page is now being displayed.  The view may be part
        // of a paretnt tabview or similar which can get de-focused.
        this.subscribeForHome()
    }

    viewIsUnfocused() {
        // an injected method from the mobile app.  Tells us that the view
        // running this page is not being displayed.  The view may be part
        // of a paretnt tabview or similar which can get de-focused.
        if (typeof this.refreshUITimer != 'undefined') {
            clearTimeout(this.refreshUITimer)
            this.refreshUITimer = undefined
        }
    }

    startSpinner() {
        this.setState({ startSpinner: true })
        this.spinnerDelayInterval = undefined
    }

    stopSpinnerTimer() {
        if (typeof this.spinnerDelayInterval != 'undefined') {
            clearTimeout(this.spinnerDelayInterval)
            this.spinnerDelayInterval = undefined
        }
    }

    formatTime(seconds) {
        const h = Math.floor(seconds / 3600)
        const m = Math.floor((seconds % 3600) / 60)
        return [
            h > 0 ? h : '0',
            m > 9 ? m : ('0' + m),
        ].filter(a => a).join(':')
    }

    render() {
        let classes = this.props.classes
        if(typeof this.state.dayUsageSummary != 'undefined') {
            this.stopSpinnerTimer()

            return(
                <div>
                    <Tabs className={classes.ScopeTabs} defaultActiveKey="day" id="scopetabs">
                        <Tab eventKey="day" title="TODAY">
                            {
                                this.state.dayUsageSummary.map(function (deviceSummary, i) {
                                    let dayUsage = this.formatTime(deviceSummary.usageDaySeconds)
                                    let averageDailyUseage = this.formatTime(deviceSummary.thirtyDayAverageSeconds)
                                    let activeLightPercentage = (deviceSummary.usageDaySeconds * 100) / this.highUsageSecondsPerDay
                                    if(activeLightPercentage > 100) {
                                        activeLightPercentage = 100
                                    }

                                    let color = PROGRESS_BAR_1

                                    if(activeLightPercentage > 80) {
                                        color = PROGRESS_BAR_5
                                    } else if (activeLightPercentage > 60) {
                                        color = PROGRESS_BAR_4
                                    } else if (activeLightPercentage > 40) {
                                        color = PROGRESS_BAR_3
                                    } else if (activeLightPercentage > 20) {
                                        color = PROGRESS_BAR_2
                                    }

                                    let utilization = 'unknown'
                                    if (deviceSummary.nextHourOnProbability >= 0) {
                                        if (deviceSummary.nextHourOnProbability > 90) {
                                            utilization = 'very active ('+deviceSummary.nextHourOnProbability+'%)'
                                        } else if (deviceSummary.nextHourOnProbability > 65) {
                                            utilization = 'active (' + deviceSummary.nextHourOnProbability + '%)'
                                        }  else if (deviceSummary.nextHourOnProbability > 35) {
                                            utilization = 'semi-active (' + deviceSummary.nextHourOnProbability + '%)'
                                        } else if (deviceSummary.nextHourOnProbability >= 5) {
                                            utilization = 'lightly used (' + deviceSummary.nextHourOnProbability + '%)'
                                        } else {
                                            utilization = 'unused (' + deviceSummary.nextHourOnProbability + '%)'
                                        }
                                    }

                                    return(
                                        <Card key={i} className={classes.FullWidthCard}>
                                            <p className={classes.Label}>{deviceSummary.deviceName}</p>
                                            <hr className={classes.LightSeparator} />

                                            <div className={classes.TimeDisplayBlock}>
                                                <div>
                                                    <div className={classes.UsageSummaryTable}>
                                                        <p className={classes.UsageSummaryDescription}>Today</p>
                                                        <p className={classes.UsageSummaryValue}>{dayUsage}</p>
                                                    </div>
                                                    <div className={classes.UsageSummaryTable}>
                                                        <p className={classes.UsageSummaryDescription}>Average Daily Usage</p>
                                                        <p className={classes.UsageSummaryValue}>{averageDailyUseage}</p>
                                                    </div>
                                                </div>

                                                <SemiCircleProgressBar
                                                    stroke={color}
                                                    diameter={ProgressBarDiameter}
                                                    strokeWidth={5}
                                                    percentage={activeLightPercentage} />
                                            </div>
                                            <hr className={classes.LightSeparator} />

                                            <div className={classes.PredictionTable}>
                                                <p className={classes.UsagePredictionSummaryDescription}>Next Hour Forecast</p>
                                                <p className={classes.UsagePredictionSummaryValue}>{utilization}</p>
                                            </div>
                                        </Card>
                                    )
                                }, this)
                            }
                        </Tab>
                        <Tab eventKey="week" title="LAST 7 DAYS">
                            {
                                this.state.weekUsageSummary.map(function (deviceSummary, i) {
                                    let weekUsage = this.formatTime(deviceSummary.usageWeekSeconds)
                                    let averageDailyUseage = this.formatTime(deviceSummary.weekAverageSeconds)
                                    let activeLightPercentage = (deviceSummary.usageWeekSeconds * 100) / (this.highUsageSecondsPerDay * 7)
                                    if (activeLightPercentage > 100) {
                                        activeLightPercentage = 100
                                    }

                                    let color = PROGRESS_BAR_1

                                    if (activeLightPercentage > 80) {
                                        color = PROGRESS_BAR_5
                                    } else if (activeLightPercentage > 60) {
                                        color = PROGRESS_BAR_4
                                    } else if (activeLightPercentage > 40) {
                                        color = PROGRESS_BAR_3
                                    } else if (activeLightPercentage > 20) {
                                        color = PROGRESS_BAR_2
                                    }

                                    return (
                                        <Card key={i} className={classes.FullWidthCard}>
                                            <p className={classes.Label}>{deviceSummary.deviceName}</p>
                                            <hr className={classes.LightSeparator} />

                                            <div className={classes.TimeDisplayBlock}>
                                                <div>
                                                    <div className={classes.UsageSummaryTable}>
                                                        <p className={classes.UsageSummaryDescription}>Last 7 Days</p>
                                                        <p className={classes.UsageSummaryValue}>{weekUsage}</p>
                                                    </div>
                                                    <div className={classes.UsageSummaryTable}>
                                                        <p className={classes.UsageSummaryDescription}>Average Daily Usage</p>
                                                        <p className={classes.UsageSummaryValue}>{averageDailyUseage}</p>
                                                    </div>
                                                </div>

                                                <SemiCircleProgressBar
                                                    stroke={color}
                                                    diameter={ProgressBarDiameter}
                                                    strokeWidth={5}
                                                    percentage={activeLightPercentage} />
                                            </div>
                                        </Card>
                                    )
                                }, this)
                            }
                        </Tab>
                   </Tabs>
                </div>
            )
        } else {
            return (<div className={classes.LoadingSpinner}>
                <FadeLoader
                    sizeUnit={"px"}
                    size={20}
                    color={TextColor}
                    loading={this.state.startSpinner} />
            </div>)
        }
    }
}
