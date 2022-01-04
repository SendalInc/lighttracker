export const TextColor = '#425e72'
export const LineSeparatorColor = '#d0d7dc'

function pxToVh(px) {
    return (px / 5.22).toString() + 'vh'
}

function pxToVw(px) {
    return (px / 3.75).toString() + 'vw'
}

export const ProgressBarDiameter = 110 * (window.innerWidth / 375.0)

export default {
    Label: {
        fontSize: pxToVw(16),
        color: TextColor,
        textAlign: 'left',
        marginLeft: pxToVw(29),
        marginTop: pxToVh(12),
        marginBottom: pxToVh(12)
    },
    PageHeader: {
        fontSize: pxToVw(30),
        letterSpacing:'-0.19vw',
        color: TextColor,
        textAlign: 'center',
        marginTop: 0
    },
    UsageSummaryDescription: {
        color: TextColor,
        textAlign: 'left',
        border: 'none',
        padding:0, 
        width: pxToVw(85),
        fontSize: pxToVw(13),
        marginBottom: pxToVh(12),
    },
    UsageSummaryValue: {
        color: TextColor,
        textAlign: 'left',
        border: 'none',
        padding: 0,
        fontSize: pxToVw(16),
        marginBottom: pxToVh(12),
    },
    UsagePredictionSummaryDescription: {
        color: TextColor,
        textAlign: 'left',
        border: 'none',
        padding: 0,
        width: pxToVw(130),
        fontSize: pxToVw(13),
        marginBottom: pxToVh(12),
    },
    UsagePredictionSummaryValue: {
        color: TextColor,
        textAlign: 'right',
        border: 'none',
        padding: 0,
        fontSize: pxToVw(13),
        marginBottom: pxToVh(12),
    },
    UsageSummaryTable: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        width: pxToVw(144),
        padding: 0,
    },
    PredictionTable: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginLeft: pxToVw(29),
        marginRight: pxToVw(29)
    },
    LoadingSpinner: {
        position: 'fixed',
        top: '40%',
        marginLeft: '5px',
        width: '100%',
        display: 'flex',
        justifyContent: 'center',
    },
    ScopeTabs: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        flexWrap: 'wrap',
        border: 'none',
        height: pxToVh(30),
        marginBottom: 0,
        padding: 0
    },
    LightSeparator: {
        color: LineSeparatorColor,
        width: pxToVw(255),
        marginTop: 0,
        marginBottom:pxToVh(8),
        marginLeft: pxToVw(29) + ' !important',
        marginRight: pxToVw(29) + ' !important'
    },
    FullWidthCard: {
        width: pxToVw(315),
        marginLeft: pxToVw(30),
        marginRight: pxToVw(30),
        marginTop: pxToVh(12),
        marginBottom: pxToVh(4),
        padding: 0,
        borderRadius: pxToVw(5),
        boxShadow: '0 ' + pxToVh(4) + ' ' + pxToVh(4) + ' 0 rgba(0, 0, 0, 0.25)',
    },
    TimeDisplayBlock: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginLeft: pxToVw(29),
        marginRight: pxToVw(29)
    }
}


