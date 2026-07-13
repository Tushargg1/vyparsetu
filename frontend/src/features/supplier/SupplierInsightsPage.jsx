import Icon from '../../components/Icon';

export default function SupplierInsightsPage() {
  return (
    <div className="space-y-lg">
      <div className="flex items-center gap-sm">
        <Icon name="auto_awesome" className="text-secondary text-[28px]" />
        <div>
          <h2 className="text-headline-lg font-bold text-on-background">AI Insights</h2>
          <p className="text-body-md text-on-surface-variant">Signals to grow your distribution network.</p>
        </div>
      </div>

      <div className="bg-surface-container-lowest rounded-2xl border border-surface-variant p-2xl text-center shadow-sm">
        <div className="mx-auto w-20 h-20 rounded-full bg-primary-fixed flex items-center justify-center mb-lg">
          <Icon name="rocket_launch" className="text-primary text-[40px]" />
        </div>
        <h3 className="text-headline-md font-bold text-on-surface mb-sm">Coming soon</h3>
        <p className="text-body-md text-on-surface-variant max-w-md mx-auto">
          Smart insights are on the way — best sellers, top retailers, order-volume trends and
          retailers that need a nudge. Stay alerted, we'll switch this on soon.
        </p>
        <div className="mt-lg inline-flex items-center gap-sm px-md py-sm rounded-full bg-secondary-container text-on-secondary-container text-label-md font-medium">
          <Icon name="notifications_active" className="text-[18px]" />
          Stay alerted
        </div>
      </div>
    </div>
  );
}
